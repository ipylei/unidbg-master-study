#include <stdio.h>
#include <signal.h>
#include <unistd.h>

void sig_handler(int sig)
{
    printf("recv the signum:%d\n", sig);
}

typedef void (*sighandler_t)(int);

int main()
{
    printf("hello world!\n");
    sighandler_t ret;
    ret = signal(SIGINT, sig_handler);
    if (ret)
    {
        printf("process signal 2 failed!\n");
    }

    sigset_t set;
    sigemptyset(&set);                  // 初始化信号集
    sigaddset(&set, SIGINT);            // 将Contrl + C信号添加到信号集
    
    sigprocmask(SIG_BLOCK, &set, NULL); // 将信号集添加到信号掩码中去
    kill(getpid(), 2);

    // do something
    // while(1);

    sigprocmask(SIG_UNBLOCK, &set, NULL);
    kill(getpid(), 2);

    return 0;
}