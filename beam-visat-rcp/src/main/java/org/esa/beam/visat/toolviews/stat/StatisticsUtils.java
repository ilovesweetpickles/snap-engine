package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.swing.figure.ShapeFigure;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TransectProfileData;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.IOException;


/**
 * Utilities for the statistics dialog.
 *
 * @author Marco Peters
 */
public class StatisticsUtils {

    public static float round(final float x) {
        return round(x, 100.0f);
    }

    public static float round(final float x, final float v) {
        return MathUtils.round(x, v);
    }

    public static double round(final double x) {
        return round(x, 100.0);
    }

    public static double round(final double x, final double v) {
        return MathUtils.round(x, v);
    }

    public static String getDiagramLabel(final RasterDataNode raster) {
        final StringBuilder sb = new StringBuilder();
        sb.append(raster.getName());
        if (StringUtils.isNotNullAndNotEmpty(raster.getUnit())) {
            sb.append(" [");
            sb.append(raster.getUnit());
            sb.append("]");
        } else {
            sb.append(" [-]");
        }
        return sb.toString();
    }

    public static class TransectProfile {

        public static TransectProfileData getTransectProfileData(final RasterDataNode raster) throws IOException {
            Shape transectShape = null;
            if(raster != null) {
                transectShape = getTransectShape(raster.getProduct());
            }
            if (transectShape == null) {
                return null;
            } else {
                return raster.createTransectProfileData(transectShape);
            }
        }

        private static Shape getTransectShape(Product product) {
            final VisatApp app = VisatApp.getApp();
            final ProductSceneView sceneView = app.getSelectedProductSceneView();
            if (sceneView != null) {
                if (sceneView.getProduct() == product) {
                    final ShapeFigure currentShapeFigure = sceneView.getCurrentShapeFigure();
                    if (currentShapeFigure != null) {
                        return currentShapeFigure.getShape();
                    }
                }
            }
            return null;
        }

        public static String createTransectProfileText(final RasterDataNode raster) throws IOException {
            final TransectProfileData data = getTransectProfileData(raster);
            if (data == null) {
                return null;
            }

            return createTransectProfileText(raster, data);
        }

        static String createTransectProfileText(RasterDataNode raster, TransectProfileData data) {
            final Point2D[] pixelPositions = data.getPixelPositions();
            final GeoPos[] geoPositions = data.getGeoPositions();
            final float[] sampleValues = data.getSampleValues();

            final StringBuilder sb = new StringBuilder(1024);
            final String formatString = "%1$-10s\t";

            sb.append(String.format(formatString, "Index"));
            sb.append(String.format(formatString, "Pixel-X"));
            sb.append(String.format(formatString, "Pixel-Y"));
            if (geoPositions != null) {
                sb.append(String.format(formatString, "Lat"));
                sb.append(String.format(formatString, "Lon"));
            }
            sb.append(String.format(formatString, getDiagramLabel(raster)));
            sb.append("\n");

            for (int i = 0; i < pixelPositions.length; i++) {
                final Point2D pixelPos = pixelPositions[i];
                sb.append(String.format(formatString, String.valueOf(i)));
                sb.append(String.format(formatString, String.valueOf(pixelPos.getX())));
                sb.append(String.format(formatString, String.valueOf(pixelPos.getY())));
                if (geoPositions != null) {
                    final GeoPos geoPos = geoPositions[i];
                    sb.append(String.format(formatString, String.valueOf(geoPos.lat)));
                    sb.append(String.format(formatString, String.valueOf(geoPos.lon)));
                }
                sb.append(String.format(formatString, String.valueOf(sampleValues[i])));
                sb.append(" \n");
            }

            return sb.toString();
        }
    }
}
