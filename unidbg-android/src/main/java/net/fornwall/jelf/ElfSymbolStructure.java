package net.fornwall.jelf;

import java.io.IOException;

//符号表
public class ElfSymbolStructure implements SymbolLocator {

    private final ElfParser parser;
    private final long offset;
    private final int entrySize;
    private final MemoizedObject<ElfStringTable> stringTable;
    private final MemoizedObject<HashTable> hashTable;

    ElfSymbolStructure(final ElfParser parser, long offset, int entrySize, MemoizedObject<ElfStringTable> stringTable, MemoizedObject<HashTable> hashTable) {
        this.parser = parser;
        this.offset = offset;        //符号表在文件中的偏移
        this.entrySize = entrySize;  //符号表单项大小
        this.stringTable = stringTable;  //字符串表
        this.hashTable = hashTable;  //哈希表
    }

    /** Returns the symbol at the specified index. The ELF symbol at index 0 is the undefined symbol. */
    @Override
    public ElfSymbol getELFSymbol(int index) throws IOException {
        return new ElfSymbol(parser, offset + (long) index * entrySize, -1).setStringTable(stringTable.getValue());
    }

    @Override
    public ElfSymbol getELFSymbolByAddr(long addr) throws IOException {
        if (hashTable == null) {
            throw new UnsupportedOperationException("hashTable is null");
        }
        return this.hashTable.getValue().findSymbolByAddress(this, addr);
    }

    @Override
    public ElfSymbol getELFSymbolByName(String name) throws IOException {
        if (hashTable == null) {
            return null;
        }
        return hashTable.getValue().getSymbol(this, name);
    }

}
