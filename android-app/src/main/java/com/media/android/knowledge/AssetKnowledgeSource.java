package com.media.android.knowledge;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reads knowledge text from the packaged Android {@code assets/} directory. */
public final class AssetKnowledgeSource implements KnowledgeSource {

    private final AssetManager assets;

    public AssetKnowledgeSource(Context context) {
        this.assets = context.getAssets();
    }

    @Override
    public List<String> list(String path) throws IOException {
        String[] names = assets.list(path);
        if (names == null) return new ArrayList<String>();
        return new ArrayList<String>(Arrays.asList(names));
    }

    @Override
    public String read(String path) throws IOException {
        try (InputStream in = assets.open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
