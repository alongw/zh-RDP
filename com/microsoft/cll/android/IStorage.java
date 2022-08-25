package com.microsoft.cll.android;

import com.microsoft.telemetry.IJsonSerializable;

import java.util.List;

public interface IStorage {
        public void add(IJsonSerializable event) throws Exception;

        public void add(Tuple<String, List<String>> event) throws Exception;

        public boolean canAdd(IJsonSerializable event);

        public boolean canAdd(Tuple<String,List<String>> event);

        public List<Tuple<String, List<String>>> drain();

        public long size();

        public void discard();

        public void close();
}
