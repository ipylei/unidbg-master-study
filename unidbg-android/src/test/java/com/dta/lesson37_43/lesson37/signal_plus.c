#include <stdio.h>
#include <signal.h>
#include <unistd.h>

void sig_handler(int sig)
{
    printf("recv the signum:%d\n", sig);
}

void sig_handler_plus(int sig, siginfo_t *info, void *ucontext)
{

    printf("recv the signum:%d data: %s\n", sig, (char *)info->si_value.sival_ptr);
}

typedef void (*sighandler_t)(int);

int main()
{
    printf("hello world!\n");
    struct sigaction act;

    // act.sa_handler = sig_handler;  //初始化，信号处理程序方式1

    act.sa_sigaction = sig_handler_plus; // 初始化，信号处理程序方式2
    act.sa_flags = SA_SIGINFO;           // 携带数据时，需要指定flag

    sigaction(SIGINT, &act, NULL); // 【设置信号处理程序方式】

    /*
    sigemptyset(&act.sa_mask);       // 初始化信号集
    sigaddset(&act.sa_mask, SIGINT); // 将Contrl + C信号添加到信号集
    sigprocmask(SIG_BLOCK, &act.sa_mask, NULL); // 将信号集添加到信号掩码中去
    */

    /*携带数据*/
    union sigval val;
    val.sival_ptr = (void *)"hello!";
    sigqueue(getpid(), SIGINT, val); // 自定义发送信号，与kill()、raise()功能一致，但可以携带数据

    while (1)
        ;
    return 0;
}