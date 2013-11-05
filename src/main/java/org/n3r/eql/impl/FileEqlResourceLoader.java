package org.n3r.eql.impl;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.n3r.eql.base.EqlResourceLoader;
import org.n3r.eql.parser.EqlBlock;
import org.n3r.eql.util.EqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static org.n3r.eql.impl.EqlResourceLoaderHelper.updateFileCache;

public class FileEqlResourceLoader extends AbstractEqlResourceLoader {
    static Logger log = LoggerFactory.getLogger(FileEqlResourceLoader.class);
    static Cache<String, Optional<Map<String, EqlBlock>>> fileCache;
    static LoadingCache<EqlUniqueSqlId, Optional<EqlBlock>> sqlCache;

    static {
        fileCache = CacheBuilder.newBuilder().build();
        sqlCache = EqlResourceLoaderHelper.buildSqlCache(fileCache);
    }

    public FileEqlResourceLoader() {
    }

    @Override
    public EqlBlock loadEqlBlock(String sqlClassPath, String sqlId) {
        load(this, sqlClassPath);

        Optional<EqlBlock> eqlBlock = sqlCache.getUnchecked(new EqlUniqueSqlId(sqlClassPath, sqlId));
        if (eqlBlock.isPresent()) return eqlBlock.get();

        throw new RuntimeException("unable to find sql id " + sqlId);
    }

    @Override
    public Map<String, EqlBlock> load(String classPath) {
        return load(this, classPath);
    }

    private Map<String, EqlBlock> load(final EqlResourceLoader eqlResourceLoader,
                                       final String sqlClassPath) {
        Callable<Optional<Map<String, EqlBlock>>> valueLoader;
        valueLoader = new Callable<Optional<Map<String, EqlBlock>>>() {
            @Override
            public Optional<Map<String, EqlBlock>> call() throws Exception {
                String sqlContent = EqlUtils.classResourceToString(sqlClassPath);
                if (sqlContent == null) {
                    log.warn("classpath sql {} not found", sqlClassPath);
                    return Optional.absent();
                }

                return Optional.of(updateFileCache(sqlContent, eqlResourceLoader, sqlClassPath));
            }
        };

        try {
            return fileCache.get(sqlClassPath, valueLoader).orNull();
        } catch (ExecutionException e) {
            Throwables.propagateIfPossible(Throwables.getRootCause(e));
        }
        return null;
    }


}
