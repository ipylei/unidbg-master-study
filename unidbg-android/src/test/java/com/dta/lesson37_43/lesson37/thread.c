#include <stdio.h>
#include <pthread.h>

// int pthread_create(pthread_t *thread, const pthread_attr_t *attr,
//                           void *(*start_routine) (void *), void *arg);
void *routine(void *arg){
    for(int i = 0; i < 10; i++){
        printf("%s: hello world%d!\n",(char *)arg, i);
    }
    return NULL;
}


int main(int argc, char **argv, char **envp){
    pthread_t tid1, tid2;
    int ret = pthread_create(&tid1, NULL, routine, "thread 1");
    if(ret){
        printf("create thread failed!\n");
        return -1;
    }
    ret = pthread_create(&tid2, NULL, routine, "thread 2");
    for(int i = 0; i < 10; i++){
        printf("%s: hello world%d!\n","thread main", i);
    }
    return 0;
}