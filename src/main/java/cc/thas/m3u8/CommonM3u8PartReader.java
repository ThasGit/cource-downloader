package cc.thas.m3u8;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


public class CommonM3u8PartReader implements Iterable<M3u8Part> {
    private final List<String> lines;

    public CommonM3u8PartReader(List<String> lines) {
        this.lines = lines;
    }

    public List<M3u8Part> toList() {
        List<M3u8Part> list = new ArrayList<>();
        for (M3u8Part defaultM3U8Part : this) {
            list.add(defaultM3U8Part);
        }
        return list;
    }

    @Override
    public Iterator<M3u8Part> iterator() {
        return new CommonM3u8PartReader.M3u8Iterator();
    }

    public M3u8PartKeyInfo resolveKey(String line) {
        return null;
    }

    private class M3u8Iterator implements Iterator<M3u8Part> {
        private int nextNum = -1;
        private int nextLineNum = -1;
        private M3u8Part nextPart;
        M3u8PartKeyInfo keyInfo;

        @Override
        public boolean hasNext() {
            findNext();
            return nextPart != null;
        }

        @Override
        public M3u8Part next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            M3u8Part curPart = nextPart;
            nextPart = null;
            return curPart;
        }

        private boolean findNext() {
            if (nextPart != null) {
                return true;
            }
            if (CollectionUtils.isEmpty(lines)) {
                return false;
            }
            while (++nextLineNum < lines.size()) {
                String curLine = lines.get(nextLineNum);
                if (curLine.startsWith("#EXT-X-KEY")) {
                    keyInfo = resolveKey(curLine);
                } else if (!curLine.startsWith("#")) {
                    nextPart = new M3u8Part(++nextNum, curLine, keyInfo);
                    return true;
                }
            }

            return false;
        }
    }
}
