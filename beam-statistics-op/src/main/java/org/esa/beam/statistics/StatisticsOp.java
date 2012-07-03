/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.statistics;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultPropertyAccessor;
import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.statistics.calculators.StatisticsCalculator;
import org.esa.beam.statistics.calculators.StatisticsCalculatorDescriptor;
import org.esa.beam.statistics.calculators.StatisticsCalculatorDescriptorRegistry;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.io.WildcardMatcher;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An operator that is used to compute statistics for any number of source products, restricted to regions given by an
 * ESRI shapefile.
 * <p/>
 * Its output is an ASCII file where the statistics are mapped to the source regions.
 * <p/>
 * Unlike most other operators, that can compute single {@link org.esa.beam.framework.gpf.Tile tiles},
 * the statistics operator processes all of its source products in its {@link #initialize()} method.
 *
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "StatisticsOp",
                  version = "1.0",
                  authors = "Thomas Storm",
                  copyright = "(c) 2012 by Brockmann Consult GmbH",
                  description = "Computes statistics for an arbitrary number of source products.")
public class StatisticsOp extends Operator implements Output {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @SourceProducts(description =
                            "The source products to be considered for statistics computation. If not given, the " +
                            "parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).")
    String[] sourceProductPaths;

    @Parameter(description = "The target file for output.", notNull = true)
    File outputFile;

    @Parameter(description =
                       "An ESRI shapefile, providing the considered geographical region(s) given as polygons. If " +
                       "null, all pixels are considered.")
    URL shapefile;

    @Parameter(description =
                       "The start date. If not given, taken from the 'oldest' source product. Products that have " +
                       "a start date before the start date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC startDate;

    @Parameter(description =
                       "The end date. If not given, taken from the 'youngest' source product. Products that have " +
                       "an end date after the end date given by this parameter are not considered.",
               format = DATETIME_PATTERN, converter = UtcConverter.class)
    ProductData.UTC endDate;

    @Parameter(description = "The band configurations. These configurations determine the output of the operator.")
    BandConfiguration[] bandConfigurations;

    Geometry[] regions;

    String[] regionIds;

    Outputter outputter;

    Set<Product> collectedProducts;

    @Override
    public void initialize() throws OperatorException {
        validateInput();
//        setUp();
        extractRegions();
        Product[] allSourceProducts = collectSourceProducts();
        initializeOutput(allSourceProducts);
        computeOutput(allSourceProducts);
        writeOutput();
    }

    private void initializeOutput(Product[] allSourceProducts) {
        final List<String> algorithmNamesList = new ArrayList<String>();
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            algorithmNamesList.add(bandConfiguration.statisticsCalculatorDescriptor.getName());
        }
        final String[] algorithmNames = algorithmNamesList.toArray(new String[algorithmNamesList.size()]);
        outputter.initialiseOutput(allSourceProducts, algorithmNames, startDate, endDate, regionIds);
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Product collectedProduct : collectedProducts) {
            collectedProduct.dispose();
        }
    }

    private void setupOutputter() {
        try {
            final PrintStream metadataOutput;
            final PrintStream csvOutput;
            if (outputFile.isDirectory()) {
                final File metadataFile = new File(outputFile, "statistics_metadata.ascii");
                final File csvFile = new File(outputFile, "statistics_data.csv");
                metadataOutput = new PrintStream(new FileOutputStream(metadataFile));
                csvOutput = new PrintStream(new FileOutputStream(csvFile));
            } else {
                final StringBuilder metadataFileName = new StringBuilder(FileUtils.getFilenameWithoutExtension(outputFile));
                metadataFileName.append("_metadata.ascii");
                final File metadataFile = new File(outputFile.getParent(), metadataFileName.toString());
                metadataOutput = new PrintStream(new FileOutputStream(metadataFile));
                csvOutput = new PrintStream(new FileOutputStream(outputFile));
            }
            this.outputter = new CsvOutputter(metadataOutput, csvOutput);
        } catch (FileNotFoundException e) {
            throw new OperatorException("Unable to create formatter for file '" + outputFile.getAbsolutePath() + "'.");
        }
    }

    void computeOutput(Product[] allSourceProducts) {
        for (BandConfiguration bandConfiguration : bandConfigurations) {
            final PropertySet propertySet = createPropertySet(bandConfiguration);
            final StatisticsCalculator statisticsCalculator = bandConfiguration.statisticsCalculatorDescriptor.createStatisticsCalculator(propertySet);
            for (int i = 0; i < regions.length; i++) {
                final Geometry region = regions[i];
                final double[] pixelValues = getPixelValues(allSourceProducts, bandConfiguration, region);
                final Map<String, Double> statistics = statisticsCalculator.calculateStatistics(pixelValues, ProgressMonitor.NULL);// todo - allow smarter progress monitor
                outputter.addToOutput(bandConfiguration, regionIds[i], statistics);
            }
        }
    }

    private static PropertySet createPropertySet(BandConfiguration bandConfiguration) {
        final PropertyContainer propertyContainer = new PropertyContainer();
        propertyContainer.addProperty(new Property(new PropertyDescriptor("percentile", Integer.class), new DefaultPropertyAccessor()));
        propertyContainer.addProperty(new Property(new PropertyDescriptor("weightCoeff", Double.class), new DefaultPropertyAccessor()));
        try {
            propertyContainer.getProperty("percentile").setValue(bandConfiguration.percentile);
            propertyContainer.getProperty("weightCoeff").setValue(bandConfiguration.weightCoeff);
        } catch (ValidationException e) {
            // Can never come here
            throw new IllegalStateException(e);
        }
        return propertyContainer;
    }

    double[] getPixelValues(Product[] products, BandConfiguration configuration, Geometry region) {
        final GeometryFactory factory = new GeometryFactory();
        final Coordinate[] coordinatesArray = {new Coordinate()};
        List<Double> buffer = new ArrayList<Double>();
        for (Product product : products) {

            Geometry pixelRegion = convertRegionToPixelRegion(region, product.getGeoCoding());

            final Shape shape = new ShapeWriter().toShape(pixelRegion);
            final Rectangle2D bounds2D = shape.getBounds2D();

            bounds2D.setRect((int) bounds2D.getX(), (int) bounds2D.getY(),
                             (int) bounds2D.getWidth() + 1, (int) bounds2D.getHeight() + 1);
            final Band band = product.getBand(configuration.sourceBandName);

            final String originalValidPixelExpression = band.getValidPixelExpression();
            band.setValidPixelExpression(configuration.validPixelExpression);

            for (int y = (int) bounds2D.getY(); y < bounds2D.getY() + bounds2D.getHeight(); y++) {
                for (int x = (int) bounds2D.getX(); x < bounds2D.getX() + bounds2D.getWidth(); x++) {
                    coordinatesArray[0].x = x;
                    coordinatesArray[0].y = y;
                    final CoordinateArraySequence coordinates = new CoordinateArraySequence(coordinatesArray);
                    final Point point = new Point(coordinates, factory);
                    final boolean contains = pixelRegion.intersects(point);

                    if (contains && band.isPixelValid(x, y)) {
                        buffer.add(ProductUtils.getGeophysicalSampleDouble(band, x, y, 0));
                    }
                }
            }

            band.setValidPixelExpression(originalValidPixelExpression);
        }
        return convertToPrimitiveArray(buffer);
    }

    private void writeOutput() {
        try {
            outputter.finaliseOutput();
        } catch (IOException e) {
            throw new OperatorException("Unable to write output.", e);
        }
    }

    private static double[] convertToPrimitiveArray(List<Double> buffer) {
        double[] pixelValues = new double[buffer.size()];
        for (int j = 0; j < buffer.size(); j++) {
            pixelValues[j] = buffer.get(j);
        }
        return pixelValues;
    }

    private static Geometry convertRegionToPixelRegion(Geometry region, final GeoCoding geoCoding) {
        // the following line does clone the coordinates, so I have to use Geometry#clone()
        // final Geometry geometry = new GeometryFactory().createGeometry(region);
        final Geometry geometry = (Geometry) region.clone();
        geometry.apply(new CoordinateFilter() {
            @Override
            public void filter(Coordinate coord) {
                final PixelPos pixelPos = new PixelPos();
                geoCoding.getPixelPos(new GeoPos((float) coord.y, (float) coord.x), pixelPos);
                coord.setCoordinate(new Coordinate((int) pixelPos.x, (int) pixelPos.y));
            }
        });
        return geometry;
    }

    void extractRegions() {
        if (shapefile == null) {
            final GeometryFactory factory = new GeometryFactory();
            regions = new Geometry[]{
                    new Polygon(new LinearRing(new CoordinateArraySequence(new Coordinate[]{
                            new Coordinate(-180, -90),
                            new Coordinate(-180, 90),
                            new Coordinate(180, 90),
                            new Coordinate(180, -90),
                            new Coordinate(-180, -90)
                    }), factory), null, factory)
            };
            regionIds = new String[]{"world"};
            return;
        }
        try {
            final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = FeatureUtils.getFeatureSource(shapefile);
            final FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource.getFeatures();
            regionIds = new String[features.size()];
            regions = new Geometry[features.size()];
            final FeatureIterator<SimpleFeature> featureIterator = features.features();
            int i = 0;
            while (featureIterator.hasNext()) {
                final SimpleFeature feature = featureIterator.next();
                final Geometry defaultGeometry = (Geometry) feature.getDefaultGeometry();
                regionIds[i] = feature.getID();
                regions[i++] = defaultGeometry;
            }
        } catch (IOException e) {
            throw new OperatorException("Unable to create URL from shapefile path '" + shapefile + "'.", e);
        }
    }

    void validateInput() {
        if (startDate != null && endDate != null && endDate.getAsDate().before(startDate.getAsDate())) {
            throw new OperatorException("End date '" + this.endDate + "' before start date '" + this.startDate + "'");
        }
        if (sourceProducts == null && (sourceProductPaths == null || sourceProductPaths.length == 0)) {
            throw new OperatorException("Either source products must be given or parameter 'sourceProductPaths' must be specified");
        }
        if(outputFile == null) {
            throw new OperatorException("Parameter 'outputFile' must not be null.");
        }
    }

    Product[] collectSourceProducts() {
        final List<Product> products = new ArrayList<Product>();
        collectedProducts = new HashSet<Product>();
        if (sourceProducts != null) {
            Collections.addAll(products, sourceProducts);
        }
        if (sourceProductPaths != null) {
            SortedSet<File> fileSet = new TreeSet<File>();
            for (String filePattern : sourceProductPaths) {
                try {
                    WildcardMatcher.glob(filePattern, fileSet);
                } catch (IOException e) {
                    logReadProductError(filePattern);
                }
            }
            for (File file : fileSet) {
                try {
                    Product sourceProduct = ProductIO.readProduct(file);
                    if (sourceProduct != null) {
                        products.add(sourceProduct);
                        collectedProducts.add(sourceProduct);
                    } else {
                        logReadProductError(file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    logReadProductError(file.getAbsolutePath());
                }
            }
        }

        return products.toArray(new Product[products.size()]);
    }

    private void logReadProductError(String file) {
        getLogger().severe(String.format("Failed to read from '%s' (not a data product or reader missing)", file));
    }


    public static class BandConfiguration {

        @Parameter(description = "The name of the band in the source products. If empty, parameter 'expression' must " +
                                 "be provided.")
        String sourceBandName;

        @Parameter(description =
                           "The band maths expression serving as input band. If empty, parameter 'sourceBandName'" +
                           "must be provided.")
        String expression;

        @Parameter(description = "The band maths expression serving as criterion for whether to consider pixels for " +
                                 "computation.")
        String validPixelExpression;

        @Parameter(description = "The name of the calculator that shall be used for this band.",
                   converter = StatisticsCalculatorDescriptorConverter.class)
        StatisticsCalculatorDescriptor statisticsCalculatorDescriptor;

        @Parameter(description = "The weight coefficient to be used in the statistics calculator, if applicable.",
                   defaultValue = "Double.NaN")
        double weightCoeff;

        @Parameter(description = "The percentile to be used in the statistics calculator, if applicable.",
                   defaultValue = "-1")
        int percentile;

    }

    public static class UtcConverter implements Converter<ProductData.UTC> {

        @Override
        public ProductData.UTC parse(String text) throws ConversionException {
            try {
                return ProductData.UTC.parse(text, DATETIME_PATTERN);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

        @Override
        public String format(ProductData.UTC value) {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public Class<ProductData.UTC> getValueType() {
            return ProductData.UTC.class;
        }

    }

    public static class StatisticsCalculatorDescriptorConverter implements Converter<StatisticsCalculatorDescriptor> {

        @Override
        public StatisticsCalculatorDescriptor parse(String text) throws ConversionException {
            final StatisticsCalculatorDescriptor descriptor = StatisticsCalculatorDescriptorRegistry.getInstance().getStatisticsCalculatorDescriptor(text);
            if (descriptor == null) {
                throw new ConversionException("No descriptor '" + text + "' registered.");
            }
            return descriptor;
        }

        @Override
        public String format(StatisticsCalculatorDescriptor value) {
            throw new IllegalStateException("Not implemented");
        }

        @Override
        public Class<StatisticsCalculatorDescriptor> getValueType() {
            return StatisticsCalculatorDescriptor.class;
        }
    }

    interface Outputter {

        void initialiseOutput(Product[] sourceProducts, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate,
                              String[] regionIds);

        void addToOutput(BandConfiguration bandConfiguration, String regionId, Map<String, Double> statistics);

        void finaliseOutput() throws IOException;
    }

    /**
     * The service provider interface (SPI) which is referenced
     * in {@code /META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(StatisticsOp.class);
        }
    }

}
