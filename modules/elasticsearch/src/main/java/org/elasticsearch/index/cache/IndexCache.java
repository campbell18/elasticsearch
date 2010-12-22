/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.cache;

import org.apache.lucene.index.IndexReader;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.common.component.CloseableComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.cache.field.data.FieldDataCache;
import org.elasticsearch.index.cache.filter.FilterCache;
import org.elasticsearch.index.cache.id.IdCache;
import org.elasticsearch.index.cache.query.parser.QueryParserCache;
import org.elasticsearch.index.settings.IndexSettings;

import javax.annotation.Nullable;

/**
 * @author kimchy (shay.banon)
 */
public class IndexCache extends AbstractIndexComponent implements CloseableComponent, ClusterStateListener {

    private final FilterCache filterCache;

    private final FieldDataCache fieldDataCache;

    private final QueryParserCache queryParserCache;

    private final IdCache idCache;

    private ClusterService clusterService;

    @Inject public IndexCache(Index index, @IndexSettings Settings indexSettings, FilterCache filterCache, FieldDataCache fieldDataCache,
                              QueryParserCache queryParserCache, IdCache idCache) {
        super(index, indexSettings);
        this.filterCache = filterCache;
        this.fieldDataCache = fieldDataCache;
        this.queryParserCache = queryParserCache;
        this.idCache = idCache;
    }

    @Inject(optional = true)
    public void setClusterService(@Nullable ClusterService clusterService) {
        this.clusterService = clusterService;
        if (clusterService != null) {
            clusterService.add(this);
        }
    }

    public FilterCache filter() {
        return filterCache;
    }

    public FieldDataCache fieldData() {
        return fieldDataCache;
    }

    public IdCache idCache() {
        return this.idCache;
    }

    public QueryParserCache queryParserCache() {
        return this.queryParserCache;
    }

    @Override public void close() throws ElasticSearchException {
        filterCache.close();
        fieldDataCache.close();
        idCache.close();
        queryParserCache.close();
        if (clusterService != null) {
            clusterService.remove(this);
        }
    }

    public void clear(IndexReader reader) {
        filterCache.clear(reader);
        fieldDataCache.clear(reader);
        idCache.clear(reader);
    }

    public void clear() {
        filterCache.clear();
        fieldDataCache.clear();
        idCache.clear();
        queryParserCache.clear();
    }

    public void clearUnreferenced() {
        filterCache.clearUnreferenced();
        fieldDataCache.clearUnreferenced();
        idCache.clearUnreferenced();
    }

    @Override public void clusterChanged(ClusterChangedEvent event) {
        // clear the query parser cache if the metadata (mappings) changed...
        if (event.metaDataChanged()) {
            queryParserCache.clear();
        }
    }
}
