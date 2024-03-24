package com.github.unidbg.linux.android.dvm;

public class StringObject extends DvmObject<String> {

    public StringObject(VM vm, String value) {
        super(vm.resolveClass("java/lang/String"), value);

        if (value == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public String toString() {
        if (value == null) {
            return null;
        } else {
            //System.err.println("-----注意length不准确!");
            //return '"' + value + '"';
            return (String) value;
        }
    }
}
