#include <stdio.h>
#include <signal.h>
#include <unistd.h>

void sig_handler(int sig){
    printf("recv the signum:%d\n", sig);
}

typedef void (*sighandler_t)(int);

int main(){
    printf("hello world!\n");
    sighandler_t ret;
    ret = signal(SIGINT, SIG_DFL);
    if(ret){
        printf("process signal 2 failed!\n");
    }
    //signal(SIGQUIT, SIG_IGN);
    ret = signal(15, sig_handler);
    if(ret){
        printf("process signal 15 failed!\n");
    }
    ret = signal(9, sig_handler);
    if(ret){
        printf("process signal 9 failed!\n");
    }
    kill(getpid(), SIGTERM);
    raise(15);
    killpg(getpid(), 15);
    //kill(getpid(), 2);
    while(1);
    return 0;
}