package fi.thl.summary.model.hydra;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import fi.thl.pivot.datasource.HydraSource;
import fi.thl.pivot.model.DimensionNode;
import fi.thl.summary.model.DataPresentation;
import fi.thl.summary.model.Presentation;
import fi.thl.summary.model.Selection;
import fi.thl.summary.model.Summary;
import fi.thl.summary.model.SummaryDimension;
import fi.thl.summary.model.SummaryItem;
import org.apache.log4j.Logger;

public class HydraDataPresentation extends DataPresentation {

    private final Logger LOG = Logger.getLogger(HydraDataPresentation.class);
    private final HydraSource source;
    private final DataPresentation delegate;
    private final Summary summary;
    private ItemFinder finder;
    private boolean emphasizedNodeCached;
    private List<DimensionNode> emphasizedNode;

    HydraDataPresentation(HydraSource source, Summary summary, Presentation p) {
        this.source = source;
        delegate = (DataPresentation) p;
        this.summary = summary;
        this.finder = new ItemFinder(summary);
    }

    public boolean isValid() {
        try {
            String url = getDataUrl();
            return url.contains("row=") || url.contains("column=");
        } catch (Exception e) {
            LOG.warn("Invalid presentation " + e.getMessage());
            return false;
        }
    }

    public String getArea() {
        String stage = "maa";
        if (((HydraSummary) summary).getGeometry() != null) {
            stage = ((HydraSummary) summary).getGeometry();
        } else {
            for (SummaryItem item : delegate.getDimensions()) {
                SummaryDimension s = ((SummaryDimension) item);
                if (s.getDimension().equals("area")) {
                    stage = s.getStage().getStage();
                    break;
                }
            }
        }
        if(stage == null) {
            stage = "MAA";
        } else if ("nutsi".equals(stage)) {
            stage = "NUTS1";
        } else if ("avi".equals(stage)) {
            stage = "ALUEHALLINTOVIRASTO";
        } else if ("ely".equals(stage)) {
            stage = "ELY-KESKUS";
        } else if ("root".equals(stage)) {
            stage = "MAA";
        } else {
            stage = stage.toUpperCase();
        }
        return stage;
    }

    public DimensionNode getMeasure() {
        if (delegate.hasMeasures()) {
            return getMeasureNodes().get(0);
        }
        return null;
    }

    public String getType() {
        return delegate.getType();
    }

    @Override
    public SortMode getSortMode() {
        return delegate.getSortMode();
    }

    @Override
    public int getGroupSize() {
        return delegate.getGroupSize();
    }

    @Override
    public boolean isLast() {
        return delegate.isLast();
    }

    @Override
    public boolean isFirst() {
        return delegate.isFirst();
    }

    /**
     * Constructs the URL to fetch data from the pivot api. Attempts to make sure
     * that both row and column attributes are used in the data query. If not
     * then the pivot API will fail.
     * 
     * @return
     */
    public String getDataUrl() {
        Set<String> closed = new HashSet<>();
        UrlBuilder url = new UrlBuilder();
        url.addRows();
        appendDimensionNodes(url, closed);
        appendMeasureItems(url, closed);
        appendFilters(url, closed);

        url.suppress(delegate.getSuppress());

        if (delegate.getShowConfidenceInterval()) {
            url.showConfidenceInterval();
        }
        if (delegate.isShowSampleSize()) {
            url.showSampleSize();
        }

        // Sorting is disabled for now as a sorted JSON-STAT data set
        // does not make sense if there are multiple more than two
        // rows and columns int total.
        // if(!SortMode.none.equals(delegate.getSortMode())) {
        // url.sort(delegate.getSortMode());
        // }

        return url.toString();

    }

    private void appendFilters(UrlBuilder url, Set<String> closed) {
        for (Selection s : getFilters()) {
            if(closed.contains(s.getDimension())) {
                continue;
            }
            if ("measure".equals(s.getDimension()) && !delegate.hasMeasures()) {
                //
            } else if (closed.size() == 2) {
                url.addColumns();
            } else {
                // We do not want extra dimension in the returned JSON-STAT
                // resource
                // so we use filters
                url.addFilters();
            }
            List<DimensionNode> nodes = IncludeDescendants.apply(s);
            if(!nodes.isEmpty()) {
                closed.add(s.getDimension());
                url.addParameter(s.getDimension(), nodes);
            }

        }
    }

    private void appendMeasureItems(UrlBuilder url, Set<String> closed) {
        if (delegate.hasMeasures() && !closed.contains("measure")) {
            url.addParameter("measure", getMeasureNodes());
            url.addColumns();
            closed.add("measure");
        }
    }

    private List<DimensionNode> getMeasureNodes() {
        return new MeasureExtension(((HydraSummary) summary), source, delegate.getMeasures()).getNodes();
    }

    private void appendDimensionNodes(UrlBuilder url, Set<String> closed) {
        for (SummaryItem d : getDimensions()) {
            Extension extension = (Extension) d;
            if(closed.contains(extension.getDimension())){
                continue;
            }

            if("map".equals(delegate.getType()) && "area".equals(extension.getDimension()) && ((HydraSummary)summary).getGeometry() != null) {
                url.addParameter(extension.getDimension(), extension.getNodes(((HydraSummary)summary).getGeometry()));
                closed.add(extension.getDimension());
            } else {
                List<DimensionNode> nodes = extension.getNodes();
                if(!nodes.isEmpty()) {
                    url.addParameter(extension.getDimension(), nodes);
                    closed.add(extension.getDimension());
                }
            }
            url.addColumns();
        }
    }

    public MeasureExtension getMeasuresExtension() {
        return new MeasureExtension((HydraSummary) summary, source, delegate.getMeasures());
    }

    public List<SummaryItem> getDimensions() {
        return extendDimensions();
    }

    private List<SummaryItem> extendDimensions() {
        return Lists.transform(delegate.getDimensions(), new ExtendDimensionFunction(((HydraSummary) summary), source));
    }

    public List<Selection> getFilters() {
        List<Selection> presentationFilters = Lists.newArrayList();
        for (Selection filter : delegate.getFilters()) {
            presentationFilters.add(summary.getSelection(filter.getId()));
        }
        return presentationFilters;
    }

    @Override
    public Integer getMin() {
        return delegate.getMin();
    }

    @Override
    public Integer getMax() {
        return delegate.getMax();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getPalette() {
        return delegate.getPalette();
    }

    @Override
    public boolean getShowConfidenceInterval() {
        return delegate.getShowConfidenceInterval();
    }

    @Override
    public List<String> getEmphasize() {
        return delegate.getEmphasize();
    }

    public List<DimensionNode> getEmphasizedNode() {
        if (emphasizedNodeCached) {
            return emphasizedNode;
        }
        if (null == getEmphasize()) {
            return null;
        }
        emphasizedNode = finder.findItems(getEmphasize(), source);
        emphasizedNodeCached = true;
        return emphasizedNode;
    }

    @Override
    public String getGeometry() {
        return delegate.getGeometry();
    }

    public boolean isVisible() {
        return new HydraRule(delegate.getRule(), ((HydraSummary) summary)).eval();
    }

}