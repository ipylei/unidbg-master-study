package com.github.unidbg.thread;

import com.github.unidbg.AbstractEmulator;
import com.github.unidbg.signal.SigSet;
import com.github.unidbg.signal.SignalOps;
import com.github.unidbg.signal.SignalTask;
import com.github.unidbg.signal.UnixSigSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 抢占式调度
 */
public class UniThreadDispatcher implements ThreadDispatcher {

    private static final Log log = LogFactory.getLog(UniThreadDispatcher.class);

    private final List<Task> taskList = new ArrayList<>();
    private final AbstractEmulator<?> emulator;

    public UniThreadDispatcher(AbstractEmulator<?> emulator) {
        this.emulator = emulator;
    }

    private final List<ThreadTask> threadTaskList = new ArrayList<>();

    @Override
    public void addThread(ThreadTask task) {
        threadTaskList.add(task);
    }

    @Override
    public List<Task> getTaskList() {
        return taskList;
    }

    @Override
    public boolean sendSignal(int tid, int sig, SignalTask signalTask) {
        List<Task> list = new ArrayList<>();
        list.addAll(taskList);
        list.addAll(threadTaskList);
        boolean ret = false;
        for (Task task : list) {
            SignalOps signalOps = null;
            if (tid == 0 && task.isMainThread()) {
                signalOps = this;
            }
            if (tid == task.getId()) {
                signalOps = task;
            }
            if (signalOps == null) {
                continue;
            }
            SigSet sigSet = signalOps.getSigMaskSet();
            SigSet sigPendingSet = signalOps.getSigPendingSet();
            if (sigPendingSet == null) {
                sigPendingSet = new UnixSigSet(0);
                signalOps.setSigPendingSet(sigPendingSet);
            }
            if (sigSet != null && sigSet.containsSigNumber(sig)) {
                sigPendingSet.addSigNumber(sig);
                return false;
            }
            if (signalTask != null) {
                task.addSignalTask(signalTask);
                if (log.isTraceEnabled()) {
                    emulator.attach().debug();
                }
            } else {
                sigPendingSet.addSigNumber(sig);
            }
            ret = true;
            break;
        }
        return ret;
    }

    private RunnableTask runningTask;

    @Override
    public RunnableTask getRunningTask() {
        return runningTask;
    }

    @Override
    public Number runMainForResult(MainTask main) {
        taskList.add(0, main);

        if (log.isDebugEnabled()) {
            log.debug("runMainForResult main=" + main);
        }

        //执行
        Number ret = run(0, null);

        //下面是在做回收工作
        for (Iterator<Task> iterator = taskList.iterator(); iterator.hasNext(); ) {
            Task task = iterator.next();
            if (task.isFinish()) {
                if (log.isDebugEnabled()) {
                    log.debug("Finish task=" + task);
                }
                task.destroy(emulator);
                iterator.remove();
                for (SignalTask signalTask : task.getSignalTaskList()) {
                    signalTask.destroy(emulator);
                    task.removeSignalTask(signalTask);
                }
            }
        }
        return ret;
    }

    @Override
    public void runThreads(long timeout, TimeUnit unit) {
        if (timeout <= 0 || unit == null) {
            throw new IllegalArgumentException("Invalid timeout.");
        }
        run(timeout, unit);
    }

    private Number run(long timeout, TimeUnit unit) {
        try {
            long start = System.currentTimeMillis();
            while (true) {
                if (taskList.isEmpty()) {
                    throw new IllegalStateException();
                }
                //遍历所有任务
                for (Iterator<Task> iterator = taskList.iterator(); iterator.hasNext(); ) {
                    Task task = iterator.next();
                    if (task.isFinish()) {
                        continue;
                    }
                    //判断任务是否可以调度
                    //当一个线程有waiter时，即需要等待另外一个线程的结果时，此时是不可调度
                    if (task.canDispatch()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Start dispatch task=" + task);
                        }
                        //代表当前正在执行的任务
                        emulator.set(Task.TASK_KEY, task);

                        //判断是否保存了上下文(即是否被中断过)
                        if (task.isContextSaved()) {
                            //恢复上下文
                            task.restoreContext(emulator);
                            //先处理信号(signal)任务 [注：信号来时一定会抛出上下文切换异常]
                            for (SignalTask signalTask : task.getSignalTaskList()) {
                                if (signalTask.canDispatch()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Start run signalTask=" + signalTask);
                                    }
                                    SignalOps ops = task.isMainThread() ? this : task;
                                    try {
                                        this.runningTask = signalTask;

                                        //【*】开始执行(信号)任务
                                        Number ret = signalTask.callHandler(ops, emulator);
                                        if (log.isDebugEnabled()) {
                                            log.debug("End run signalTask=" + signalTask + ", ret=" + ret);
                                        }
                                        if (ret != null) {
                                            signalTask.setResult(emulator, ret);
                                            signalTask.destroy(emulator);
                                            task.removeSignalTask(signalTask);
                                        } else {
                                            signalTask.saveContext(emulator);
                                        }
                                    } catch (PopContextException e) {
                                        this.runningTask.popContext(emulator);
                                    }
                                } else if (log.isDebugEnabled()) {
                                    log.debug("Skip call handler signalTask=" + signalTask);
                                }
                            }
                        }

                        //信号(signal)任务处理完后，才接着处理程序基本任务
                        try {
                            this.runningTask = task;

                            //【*】开始执行任务(null代表当前线程\任务没有执行完，下面会进行上下文保存)
                            Number ret = task.dispatch(emulator);

                            if (log.isDebugEnabled()) {
                                log.debug("End dispatch task=" + task + ", ret=" + ret);
                            }
                            if (ret != null) {
                                task.setResult(emulator, ret);
                                task.destroy(emulator);
                                iterator.remove();
                                if (task.isMainThread()) {
                                    return ret;
                                }
                            } else {
                                //进行上下文保存
                                task.saveContext(emulator);
                            }
                        } catch (PopContextException e) {
                            this.runningTask.popContext(emulator);
                        }
                    } else {
                        if (log.isTraceEnabled() && task.isContextSaved()) {
                            task.restoreContext(emulator);
                            log.trace("Skip dispatch task=" + task);
                            emulator.getUnwinder().unwind();
                        } else if (log.isDebugEnabled()) {
                            log.debug("Skip dispatch task=" + task);
                        }
                    }
                }

                Collections.reverse(threadTaskList);
                //将线程任务也添加到任务列表中
                for (Iterator<ThreadTask> iterator = threadTaskList.iterator(); iterator.hasNext(); ) {
                    taskList.add(0, iterator.next());
                    iterator.remove();
                }

                if (timeout > 0 && unit != null && System.currentTimeMillis() - start >= unit.toMillis(timeout)) {
                    return null;
                }
                //一般不会走这里，这是一个非正常的情况
                if (taskList.isEmpty()) {
                    return null;
                }

                if (log.isDebugEnabled()) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        } finally {
            this.runningTask = null;
            emulator.set(Task.TASK_KEY, null);
        }
    }

    @Override
    public int getTaskCount() {
        return taskList.size() + threadTaskList.size();
    }

    private SigSet mainThreadSigMaskSet;
    private SigSet mainThreadSigPendingSet;

    @Override
    public SigSet getSigMaskSet() {
        return mainThreadSigMaskSet;
    }

    @Override
    public void setSigMaskSet(SigSet sigMaskSet) {
        this.mainThreadSigMaskSet = sigMaskSet;
    }

    @Override
    public SigSet getSigPendingSet() {
        return mainThreadSigPendingSet;
    }

    @Override
    public void setSigPendingSet(SigSet sigPendingSet) {
        this.mainThreadSigPendingSet = sigPendingSet;
    }
}
