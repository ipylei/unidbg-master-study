package com.dta.utils;

import com.github.unidbg.Alignment;
import com.github.unidbg.arm.ARM;

public class Main {

    public static void main(String[] args) {
        //long l = ARM.alignSize(1, 4096);
        //System.out.println(l);

        Alignment align = ARM.align(4090, 1, 4096); // 起始:0，占据1页(size:4096)
        //Alignment align = ARM.align(4090, 7, 4096); //起始:0，占据2页(size:8092)
        //Alignment align = ARM.align(0x1020, 100, 4096); //起始:4096，占据1页(size:4096)
        System.out.println(align.address);
        System.out.println(align.size);


        long l = ARM.alignSize(4095, 4096);
        //long l = ARM.alignSize(4097, 4096);
        //long l = ARM.alignSize(0x1020, 4096);
        System.out.println(l);
    }
}
