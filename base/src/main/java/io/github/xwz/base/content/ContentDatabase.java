package io.github.xwz.base.content;

import com.raizlabs.android.dbflow.annotation.Database;

@Database(name = ContentDatabase.NAME, version = ContentDatabase.VERSION)
public class ContentDatabase {
    public static final String NAME = "Content";
    public static final int VERSION = 1;
}