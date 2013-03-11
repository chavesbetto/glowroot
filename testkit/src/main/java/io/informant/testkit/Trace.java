/**
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.informant.testkit;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import checkers.nullness.quals.Nullable;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
public class Trace {

    @Nullable
    private String id;
    private long start;
    private long duration;
    // active is slightly different from !completed because a trace is stored at the stuck threshold
    // and the jvm may terminate before that trace completes, in which case the trace is
    // neither active nor completed
    private boolean active;
    private boolean stuck;
    private boolean completed;
    private boolean background;
    @Nullable
    private String headline;
    @Nullable
    private Map<String, String> attributes;
    @Nullable
    private String userId;
    @Nullable
    private TraceError error;
    @Nullable
    private List<Metric> metrics;
    @Nullable
    private List<Span> spans;
    @Nullable
    private MergedStackTreeNode coarseMergedStackTree;
    @Nullable
    private MergedStackTreeNode fineMergedStackTree;

    private boolean summary;

    @Nullable
    public String getId() {
        return id;
    }

    public long getStart() {
        return start;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isStuck() {
        return stuck;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isBackground() {
        return background;
    }

    @Nullable
    public String getHeadline() {
        return headline;
    }

    @Nullable
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public TraceError getError() {
        return error;
    }

    @Nullable
    public List<Metric> getMetrics() {
        return getStableAndOrderedMetrics();
    }

    @Nullable
    public List<String> getMetricNames() {
        List<Metric> stableMetrics = getStableAndOrderedMetrics();
        if (stableMetrics == null) {
            return null;
        }
        List<String> stableMetricNames = Lists.newArrayList();
        for (Metric stableMetric : stableMetrics) {
            String name = stableMetric.getName();
            if (name == null) {
                throw new IllegalStateException("Found metric with null name");
            }
            stableMetricNames.add(name);
        }
        return stableMetricNames;
    }

    @Nullable
    public List<Span> getSpans() {
        if (summary) {
            throw new IllegalStateException("Use Informant.getLastTrace() instead of"
                    + " Informant.getLastTraceSummary() to retrieve spans");
        }
        return spans;
    }

    @Nullable
    public MergedStackTreeNode getCoarseMergedStackTree() {
        if (summary) {
            throw new IllegalStateException("Use Informant.getLastTrace() instead of"
                    + " Informant.getLastTraceSummary() to retrieve mergedStackTree");
        }
        return coarseMergedStackTree;
    }

    @Nullable
    public MergedStackTreeNode getFineMergedStackTree() {
        if (summary) {
            throw new IllegalStateException("Use Informant.getLastTrace() instead of"
                    + " Informant.getLastTraceSummary() to retrieve mergedStackTree");
        }
        return fineMergedStackTree;
    }

    void setSummary(boolean summary) {
        this.summary = summary;
    }

    // the informant weaving metric is a bit unpredictable since tests are often run inside the
    // same InformantContainer for test speed, so test order affects whether any classes are
    // woven during the test or not
    // it's easiest to just ignore this metric completely
    @Nullable
    private List<Metric> getStableAndOrderedMetrics() {
        if (metrics == null) {
            return null;
        }
        List<Metric> stableMetrics = Lists.newArrayList(metrics);
        for (Iterator<Metric> i = stableMetrics.iterator(); i.hasNext();) {
            if ("informant weaving".equals(i.next().getName())) {
                i.remove();
            }
        }
        return Metric.orderingByTotal.reverse().sortedCopy(stableMetrics);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("start", start)
                .add("duration", duration)
                .add("active", active)
                .add("stuck", stuck)
                .add("completed", completed)
                .add("background", background)
                .add("headline", headline)
                .add("attributes", attributes)
                .add("userId", userId)
                .add("error", error)
                .add("metrics", metrics)
                .add("spans", spans)
                .add("coarseMergedStackTree", coarseMergedStackTree)
                .add("fineMergedStackTree", fineMergedStackTree)
                .toString();
    }

    public static class TraceError {
        @Nullable
        private String text;
        @Nullable
        private Map<String, Object> detail;
        @Nullable
        private ExceptionInfo exception;

        @Nullable
        public String getText() {
            return text;
        }
        @Nullable
        public Map<String, Object> getDetail() {
            return detail;
        }
        @Nullable
        public ExceptionInfo getException() {
            return exception;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("text", text)
                    .add("detail", detail)
                    .add("exception", exception)
                    .toString();
        }
    }

    public static class ExceptionInfo {

        @Nullable
        private String display;
        @Nullable
        private List<String> stackTrace;
        private int framesInCommonWithCaused;
        @Nullable
        private ExceptionInfo cause;

        @Nullable
        public String getDisplay() {
            return display;
        }
        public void setDisplay(String display) {
            this.display = display;
        }
        @Nullable
        public List<String> getStackTrace() {
            return stackTrace;
        }
        public void setStackTrace(List<String> stackTrace) {
            this.stackTrace = stackTrace;
        }
        public int getFramesInCommonWithCaused() {
            return framesInCommonWithCaused;
        }
        public void setFramesInCommonWithCaused(int framesInCommonWithCaused) {
            this.framesInCommonWithCaused = framesInCommonWithCaused;
        }
        @Nullable
        public ExceptionInfo getCause() {
            return cause;
        }
        public void setCause(@Nullable ExceptionInfo cause) {
            this.cause = cause;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("display", display)
                    .add("stackTrace", stackTrace)
                    .add("framesInCommonWithCaused", framesInCommonWithCaused)
                    .add("cause", cause)
                    .toString();
        }
    }

    public static class Metric {

        private static final Ordering<Metric> orderingByTotal = new Ordering<Metric>() {
            @Override
            public int compare(Metric left, Metric right) {
                return Longs.compare(left.total, right.total);
            }
        };

        @Nullable
        private String name;
        private long total;
        private long min;
        private long max;
        private long count;
        private boolean active;
        private boolean minActive;
        private boolean maxActive;

        @Nullable
        public String getName() {
            return name;
        }
        public long getTotal() {
            return total;
        }
        public long getMin() {
            return min;
        }
        public long getMax() {
            return max;
        }
        public long getCount() {
            return count;
        }
        public boolean isActive() {
            return active;
        }
        public boolean isMinActive() {
            return minActive;
        }
        public boolean isMaxActive() {
            return maxActive;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("name", name)
                    .add("total", total)
                    .add("min", min)
                    .add("max", max)
                    .add("count", count)
                    .add("isActive", active)
                    .add("minActive", minActive)
                    .add("maxActive", maxActive)
                    .toString();
        }
    }

    public static class Span {

        private long offset;
        private long duration;
        private boolean active;
        private int index;
        private int nestingLevel;
        // message is null for spans created via PluginServices.addErrorSpan()
        @Nullable
        private Message message;
        @Nullable
        private ErrorMessage error;
        @Nullable
        private List<String> stackTrace;
        private boolean limitExceededMarker;
        private boolean limitExtendedMarker;

        public long getOffset() {
            return offset;
        }
        public long getDuration() {
            return duration;
        }
        public boolean isActive() {
            return active;
        }
        public int getIndex() {
            return index;
        }
        public int getNestingLevel() {
            return nestingLevel;
        }
        @Nullable
        public Message getMessage() {
            return message;
        }
        @Nullable
        public ErrorMessage getError() {
            return error;
        }
        @Nullable
        public List<String> getStackTrace() {
            return stackTrace;
        }
        public boolean isLimitExceededMarker() {
            return limitExceededMarker;
        }
        public boolean isLimitExtendedMarker() {
            return limitExtendedMarker;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("offset", offset)
                    .add("duration", duration)
                    .add("active", active)
                    .add("index", index)
                    .add("nestingLevel", nestingLevel)
                    .add("message", message)
                    .add("error", error)
                    .add("stackTrace", stackTrace)
                    .add("limitExceededMarker", limitExceededMarker)
                    .add("limitExtendedMarker", limitExtendedMarker)
                    .toString();
        }
    }

    public static class Message {

        @Nullable
        private String text;
        @Nullable
        private Map<String, Object> detail;

        @Nullable
        public String getText() {
            return text;
        }
        @Nullable
        public Map<String, Object> getDetail() {
            return detail;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("text", text)
                    .add("detail", detail)
                    .toString();
        }
    }

    public static class ErrorMessage extends Message {

        @Nullable
        private ExceptionInfo exception;

        @Nullable
        public ExceptionInfo getException() {
            return exception;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("text", getText())
                    .add("detail", getDetail())
                    .add("exception", exception)
                    .toString();
        }
    }

    public static class MergedStackTreeNode {

        @Nullable
        private String stackTraceElement;
        @Nullable
        private List<MergedStackTreeNode> childNodes;
        @Nullable
        private List<String> metricNames;
        private int sampleCount;
        @Nullable
        private String leafThreadState;

        // null for synthetic root only
        @Nullable
        public String getStackTraceElement() {
            return stackTraceElement;
        }
        @Nullable
        public List<MergedStackTreeNode> getChildNodes() {
            return childNodes;
        }
        @Nullable
        public List<String> getMetricNames() {
            return metricNames;
        }
        public int getSampleCount() {
            return sampleCount;
        }
        @Nullable
        public String getLeafThreadState() {
            return leafThreadState;
        }
        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("stackTraceElement", stackTraceElement)
                    .add("childNodes", childNodes)
                    .add("metricNames", metricNames)
                    .add("sampleCount", sampleCount)
                    .add("leafThreadState", leafThreadState)
                    .toString();
        }
    }
}
