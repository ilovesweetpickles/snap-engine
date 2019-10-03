package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.tao.AbstractTAORemoteRepositoryProvider;
import ro.cs.tao.datasource.remote.ProductHelper;
import ro.cs.tao.datasource.remote.scihub.SciHubDataSource;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.products.sentinels.SentinelProductHelper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class SciHubProductsRepositoryProvider extends AbstractTAORemoteRepositoryProvider<SciHubDataSource> {

    private static final Map<String, String> DISPLAYED_ATTRIBUTES = new LinkedHashMap<>();
    static {
        DISPLAYED_ATTRIBUTES.put("producttype", "Product Type");
        DISPLAYED_ATTRIBUTES.put("instrumentshortname", "Instrument");
    }

    public SciHubProductsRepositoryProvider() {
    }

    @Override
    protected SciHubRepositoryProduct buildRepositoryProduct(EOProduct product, String mission, Polygon2D polygon) {
        return new SciHubRepositoryProduct(product, mission, polygon);
    }

    @Override
    protected ProductHelper buildProductHelper(String productName) {
        return SentinelProductHelper.create(productName);
    }

    @Override
    protected Class<SciHubDataSource> getDataSourceClass() {
        return SciHubDataSource.class;
    }

    @Override
    public String getRepositoryName() {
        return "ESA SciHub";
    }

    @Override
    public ProductRepositoryDownloader buidProductDownloader(String mission) {
        return new SciHubProductRepositoryDownloader(mission, getRepositoryId());
    }

    @Override
    public Map<String, String> getDisplayedAttributes() {
        return DISPLAYED_ATTRIBUTES;
    }
}