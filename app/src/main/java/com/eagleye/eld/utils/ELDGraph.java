package com.eagleye.eld.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* renamed from: com.nationwideeld.android.ui.others.ELDGraph */
/* compiled from: ELDGraph.kt */
public final class ELDGraph extends View {
    public static final int $stable = 8;
    private Canvas canvas;
    private List<? extends ELDGraphData> eldGraphDataList;
    private final Paint graphPlotPaint;
    private final Paint offDutyBackgroundPaint;
    private final Paint sbBackgroundPaint;
    private final Paint drivingBackgroundPaint;
    private final Paint onDutyBackgroundPaint;
    private final int numColumns;
    private final int numRows;
    private final Paint whitePaint;
    private final Paint timeLabelPaint;
    private final Paint dotPaint;
    private final float[] lastLabelRightByRow;
    private final int[] labelStackLevelByRow;
    // Meta totals from server (in seconds); -1 means not set
    private int metaOffSec = -1;
    private int metaSbSec = -1;
    private int metaDSec = -1;
    private int metaOnSec = -1;

    /*
     * JADX INFO: super call moved to the top of the method (can break code
     * semantics)
     */
    public ELDGraph(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Intrinsics.checkNotNullParameter(context, "context");

        Paint paint = new Paint();
        this.whitePaint = paint;
        Paint paint2 = new Paint();
        this.graphPlotPaint = paint2;

        // Initialize background paints for each duty status
        Paint offBgPaint = new Paint();
        this.offDutyBackgroundPaint = offBgPaint;
        Paint sbBgPaint = new Paint();
        this.sbBackgroundPaint = sbBgPaint;
        Paint driveBgPaint = new Paint();
        this.drivingBackgroundPaint = driveBgPaint;
        Paint onBgPaint = new Paint();
        this.onDutyBackgroundPaint = onBgPaint;

        // Time label paint - black text drawn above the line at segment start
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.timeLabelPaint = labelPaint;
        labelPaint.setColor(Color.BLACK);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(22f);
        labelPaint.setFakeBoldText(true);

        // Green dot paint for transition points
        Paint dp = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.dotPaint = dp;
        dp.setColor(Color.parseColor("#00CC00")); // bright green
        dp.setStyle(Paint.Style.FILL);

        this.numColumns = 24;
        this.numRows = 4;
        this.lastLabelRightByRow = new float[this.numRows + 1];
        this.labelStackLevelByRow = new int[this.numRows + 1];
        this.eldGraphDataList = new ArrayList();

        // Configure grid lines paint
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.parseColor("#333333")); // Dark gray for better visibility on colored backgrounds
        paint.setStrokeWidth(1.0f);

        // Configure graph plot paint
        paint2.setStyle(Paint.Style.FILL_AND_STROKE);
        paint2.setStrokeWidth(2.0f); // Normal thickness for status lines
        paint2.setColor(Color.parseColor("#000000")); // Black lines for maximum contrast

        // Configure background paints with colors from image
        offBgPaint.setStyle(Paint.Style.FILL);
        offBgPaint.setColor(Color.parseColor("#87CEEB")); // Light blue for OFF duty

        sbBgPaint.setStyle(Paint.Style.FILL);
        sbBgPaint.setColor(Color.parseColor("#4169E1")); // Royal blue for Sleeper Berth

        driveBgPaint.setStyle(Paint.Style.FILL);
        driveBgPaint.setColor(Color.parseColor("#FFA500")); // Orange for Driving

        onBgPaint.setStyle(Paint.Style.FILL);
        onBgPaint.setColor(Color.parseColor("#FFD700")); // Gold/Yellow for ON duty
    }

    private void resetLabelLayoutState() {
        Arrays.fill(this.lastLabelRightByRow, Float.NEGATIVE_INFINITY);
        Arrays.fill(this.labelStackLevelByRow, 0);
    }

    public ELDGraph(Context context, AttributeSet attributeSet, int i,
            DefaultConstructorMarker defaultConstructorMarker) {
        this(context, (i & 2) != 0 ? null : attributeSet);
    }

    public final void plotGraph(List<? extends ELDGraphData> list) {
        Intrinsics.checkNotNullParameter(list, "eldGraphDataList");
        this.eldGraphDataList = list;
    }

    /** Call this overload to also supply server meta totals (seconds). */
    public final void plotGraph(List<? extends ELDGraphData> list,
            int offSec, int sbSec, int dSec, int onSec) {
        Intrinsics.checkNotNullParameter(list, "eldGraphDataList");
        this.eldGraphDataList = list;
        this.metaOffSec = offSec;
        this.metaSbSec = sbSec;
        this.metaDSec = dSec;
        this.metaOnSec = onSec;
    }

    private final void changeDutyStatus(int i, int i2, float f) {
        // Timber.Forest forest = Timber.Forest;
        // forest.mo46574i("change duty -> " + i + ' ' + i2 + ' ' + f, new Object[0]);
        Canvas canvas2 = this.canvas;
        if (canvas2 != null) {
            canvas2.drawLine((((float) getWidth()) * f) / ((float) this.numColumns),
                    (float) (((double) ((i * getHeight()) / this.numRows))
                            - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))),
                    (f * ((float) getWidth())) / ((float) this.numColumns),
                    (float) (((double) ((i2 * getHeight()) / this.numRows))
                            - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))),
                    this.graphPlotPaint);
        }
    }

    private final void drawStatusLine(float f, float f2, int i) {
        // Timber.Forest forest = Timber.Forest;
        // forest.mo46574i("draw line -> " + f + ' ' + f2 + ' ' + i, new Object[0]);
        Canvas canvas2 = this.canvas;
        if (canvas2 != null) {
            canvas2.drawLine((f * ((float) getWidth())) / ((float) this.numColumns),
                    (float) (((double) ((getHeight() * i) / this.numRows))
                            - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))),
                    (f2 * ((float) getWidth())) / ((float) this.numColumns),
                    (float) (((double) ((i * getHeight()) / this.numRows))
                            - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))),
                    this.graphPlotPaint);
        }
    }

    private final void drawRowBackground(int rowIndex) {
        Canvas canvas2 = this.canvas;
        if (canvas2 != null) {
            Paint backgroundPaint;
            switch (rowIndex) {
                case 1: // OFF duty
                    backgroundPaint = this.offDutyBackgroundPaint;
                    break;
                case 2: // Sleeper Berth
                    backgroundPaint = this.sbBackgroundPaint;
                    break;
                case 3: // Driving
                    backgroundPaint = this.drivingBackgroundPaint;
                    break;
                case 4: // ON duty
                    backgroundPaint = this.onDutyBackgroundPaint;
                    break;
                default:
                    return; // Don't draw background for invalid rows
            }

            // Calculate row boundaries
            float top = (float) ((getHeight() * (rowIndex - 1)) / this.numRows);
            float bottom = (float) ((getHeight() * rowIndex) / this.numRows);

            // Draw the colored background for the entire row
            canvas2.drawRect(0.0f, top, (float) getWidth(), bottom, backgroundPaint);
        }
    }

    /**
     * Draw a HH:MM duration label just above the START point of the segment line.
     * This matches the reference style where labels appear above the line at the
     * start.
     *
     * @param startTime segment start in hours (0-24 float)
     * @param endTime   segment end in hours (0-24 float)
     * @param rowIndex  1=OFF, 2=SB, 3=DR, 4=ON
     */
    private final void drawSegmentTimeLabel(float startTime, float endTime, int rowIndex) {
        Canvas canvas2 = this.canvas;
        if (canvas2 == null || rowIndex == 0)
            return;

        float durationHours = endTime - startTime;
        if (durationHours <= 0f)
            return;

        // Convert duration to HH:MM using floor to avoid rounding accumulation
        // Multiply by 3600 to get seconds, then extract hours and minutes
        int totalSeconds = (int) (durationHours * 3600f);
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        // Round up the minutes if remaining seconds >= 30
        int remainSec = totalSeconds % 60;
        if (remainSec >= 30)
            minutes = Math.min(59, minutes + 1);
        String label = String.format("%02d:%02d", hours, minutes);

        // Reduce font size for narrow segments, but never skip the label.
        float segmentWidthPx = (durationHours * (float) getWidth()) / (float) this.numColumns;
        float originalSize = timeLabelPaint.getTextSize();
        float textWidth = timeLabelPaint.measureText(label);
        if (segmentWidthPx < textWidth + 4f) {
            float scale = (segmentWidthPx - 4f) / textWidth;
            float boundedScale = Math.max(0.35f, scale);
            float newSize = Math.max(8f, originalSize * boundedScale);
            timeLabelPaint.setTextSize(newSize);
            textWidth = timeLabelPaint.measureText(label);
        }

        // For tiny segments, anchor near start so even 1-minute segments are visible.
        float xCenter;
        if (segmentWidthPx < textWidth + 2f) {
            float segmentStartX = (startTime * (float) getWidth()) / (float) this.numColumns;
            xCenter = segmentStartX + (textWidth / 2f) + 2f;
        } else {
            xCenter = ((startTime + endTime) / 2f * (float) getWidth()) / (float) this.numColumns;
        }

        float minX = (textWidth / 2f) + 2f;
        float maxX = ((float) getWidth()) - (textWidth / 2f) - 2f;
        xCenter = Math.max(minX, Math.min(maxX, xCenter));

        // Y position: just above the horizontal line of this row
        float lineY = (float) (((double) ((getHeight() * rowIndex) / this.numRows))
                - ((((double) getHeight()) * 0.5d) / ((double) this.numRows)));

        // Draw label just above the line (8px gap)
        Paint.FontMetrics fm = timeLabelPaint.getFontMetrics();
        float yBaseline = lineY - 8f - fm.descent;

        int safeRowIndex = Math.max(0, Math.min(rowIndex, this.numRows));
        float labelLeft = xCenter - (textWidth / 2f);
        float labelRight = xCenter + (textWidth / 2f);
        if (safeRowIndex < this.lastLabelRightByRow.length) {
            if (labelLeft <= this.lastLabelRightByRow[safeRowIndex] + 2f) {
                this.labelStackLevelByRow[safeRowIndex] = Math.min(this.labelStackLevelByRow[safeRowIndex] + 1, 3);
            } else {
                this.labelStackLevelByRow[safeRowIndex] = 0;
            }
            float verticalStep = (fm.bottom - fm.top) + 2f;
            yBaseline -= this.labelStackLevelByRow[safeRowIndex] * verticalStep;
            this.lastLabelRightByRow[safeRowIndex] = labelRight;
        }

        canvas2.drawText(label, xCenter, yBaseline, timeLabelPaint);

        // Reset font size for next segment
        timeLabelPaint.setTextSize(originalSize);
    }

    /**
     * Draw a green dot at a transition/start point on the graph line.
     *
     * @param timeHour time in hours (0-24 float)
     * @param rowIndex 1=OFF, 2=SB, 3=DR, 4=ON
     */
    private final void drawDot(float timeHour, int rowIndex) {
        Canvas canvas2 = this.canvas;
        if (canvas2 == null || rowIndex == 0)
            return;

        float x = (timeHour * (float) getWidth()) / (float) this.numColumns;
        float y = (float) (((double) ((getHeight() * rowIndex) / this.numRows))
                - ((((double) getHeight()) * 0.5d) / ((double) this.numRows)));

        canvas2.drawCircle(x, y, 6f, dotPaint);
    }

    /**
     * Draw the server-provided total HH:MM label centered in a mode row.
     * rowIndex: 1=OFF, 2=SB, 3=DR, 4=ON
     * totalSec: total seconds for that mode (-1 = not available, skip)
     */
    private final void drawRowTotalLabel(int rowIndex, int totalSec) {
        Canvas canvas2 = this.canvas;
        if (canvas2 == null || totalSec < 0)
            return;

        int hours = totalSec / 3600;
        int minutes = (totalSec % 3600) / 60;
        String label = String.format("%02d:%02d", hours, minutes);

        // Center of the row vertically
        float rowTop = (float) ((getHeight() * (rowIndex - 1)) / this.numRows);
        float rowBottom = (float) ((getHeight() * rowIndex) / this.numRows);
        float yCenter = rowTop + (rowBottom - rowTop) / 2f;

        // Horizontally: center of the entire graph width
        float xCenter = (float) getWidth() / 2f;

        Paint.FontMetrics fm = timeLabelPaint.getFontMetrics();
        float yBaseline = yCenter - (fm.ascent + fm.descent) / 2f;

        canvas2.drawText(label, xCenter, yBaseline, timeLabelPaint);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas2) {
        int i;
        int i2;
        Canvas canvas3 = canvas2;
        super.onDraw(canvas2);
        this.canvas = canvas3;
        if (canvas3 != null) {
            canvas3.drawColor(Color.WHITE); // Changed from black to white background
        }

        // Draw colored backgrounds for each duty status row
        drawRowBackground(1); // OFF duty - light blue
        drawRowBackground(2); // Sleeper Berth - dark blue
        drawRowBackground(3); // Driving - orange
        drawRowBackground(4); // ON duty - yellow
        if (canvas3 != null) {
            canvas2.drawLine(0.0f, 0.0f, (float) getWidth(), 0.0f, this.whitePaint);
        }
        if (canvas3 != null) {
            canvas2.drawLine(0.0f, 0.0f, 0.0f, (float) getHeight(), this.whitePaint);
        }
        if (canvas3 != null) {
            canvas2.drawLine(0.0f, (float) (getHeight() - 1), (float) getWidth(), (float) (getHeight() - 1),
                    this.whitePaint);
        }
        if (canvas3 != null) {
            canvas2.drawLine((float) (getWidth() - 1), (float) getHeight(), (float) (getWidth() - 1), 0.0f,
                    this.whitePaint);
        }
        int i3 = this.numRows;
        int i4 = 0;
        for (int i5 = 0; i5 < i3; i5++) {
            if (canvas3 != null) {
                canvas2.drawLine(0.0f, (float) ((getHeight() * i5) / this.numRows), (float) getWidth(),
                        (float) ((getHeight() * i5) / this.numRows), this.whitePaint);
            }
        }
        int i6 = this.numColumns;
        for (int i7 = 0; i7 < i6; i7++) {
            if (canvas3 != null) {
                canvas2.drawLine((float) ((getWidth() * i7) / this.numColumns), 0.0f,
                        (float) ((getWidth() * i7) / this.numColumns), (float) getHeight(), this.whitePaint);
            }
        }
        int i8 = this.numRows;
        if (i8 >= 0) {
            int i9 = 0;
            while (true) {
                int i10 = this.numColumns;
                if (i10 >= 0) {
                    int i11 = 0;
                    while (true) {
                        if (canvas3 != null) {
                            i2 = i8;
                            canvas2.drawLine(
                                    (float) (((double) ((getWidth() * i11) / this.numColumns))
                                            + ((((double) getWidth()) * 0.5d) / ((double) this.numColumns))),
                                    (float) ((getHeight() * i9) / this.numRows),
                                    (float) (((double) ((getWidth() * i11) / this.numColumns))
                                            + ((((double) getWidth()) * 0.5d) / ((double) this.numColumns))),
                                    (float) (((double) ((getHeight() * i9) / this.numRows))
                                            - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))),
                                    this.whitePaint);
                        } else {
                            i2 = i8;
                        }
                        if (canvas3 != null) {
                            canvas2.drawLine(
                                    (float) (((double) ((getWidth() * i11) / this.numColumns))
                                            + ((((double) getWidth()) * 0.25d) / ((double) this.numColumns))),
                                    (float) ((getHeight() * i9) / this.numRows),
                                    (float) (((double) ((getWidth() * i11) / this.numColumns))
                                            + ((((double) getWidth()) * 0.25d) / ((double) this.numColumns))),
                                    (float) (((double) ((getHeight() * i9) / this.numRows))
                                            - ((((double) getHeight()) * 0.25d) / ((double) this.numRows))),
                                    this.whitePaint);
                        }
                        if (canvas3 != null) {
                            canvas2.drawLine(
                                    (float) (((double) ((getWidth() * i11) / this.numColumns))
                                            + ((((double) getWidth()) * 0.75d) / ((double) this.numColumns))),
                                    (float) ((getHeight() * i9) / this.numRows),
                                    (float) (((double) ((getWidth() * i11) / this.numColumns))
                                            + ((((double) getWidth()) * 0.75d) / ((double) this.numColumns))),
                                    (float) (((double) ((getHeight() * i9) / this.numRows))
                                            - ((((double) getHeight()) * 0.25d) / ((double) this.numRows))),
                                    this.whitePaint);
                        }
                        if (i11 == i10) {
                            break;
                        }
                        i11++;
                        i8 = i2;
                    }
                    i = i2;
                } else {
                    i = i8;
                }
                if (i9 == i) {
                    break;
                }
                i9++;
                i8 = i;
            }
        }
        Collection arrayList = new ArrayList();
        resetLabelLayoutState();
        for (Object next : this.eldGraphDataList) {
            ELDGraphData eLDGraphData = (ELDGraphData) next;
            // Include ALL duty statuses, don't exclude E_ON and E_OFF
            arrayList.add(next);
        }
        int lastValidDutyStatus = 1; // Default to OFF if none found
        for (Object next2 : (List) arrayList) {
            int i12 = i4 + 1;
            if (i4 < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            ELDGraphData eLDGraphData2 = (ELDGraphData) next2;
            int currentStatus = convertEventNameToStatus(((ELDGraphData) this.eldGraphDataList.get(i4)).getStatus());

            // Handle standard duty statuses
            if (currentStatus >= 1 && currentStatus <= 4) {
                lastValidDutyStatus = currentStatus;

                // Find next standard duty status to draw horizontal line
                int nextValidIdx = i12;
                float segStart = ((ELDGraphData) this.eldGraphDataList.get(i4)).getTime();
                float segEnd = segStart;

                while (nextValidIdx < this.eldGraphDataList.size()) {
                    int nextStatus = convertEventNameToStatus(
                            ((ELDGraphData) this.eldGraphDataList.get(nextValidIdx)).getStatus());
                    segEnd = ((ELDGraphData) this.eldGraphDataList.get(nextValidIdx)).getTime();
                    if (nextStatus >= 1 && nextStatus <= 4) {
                        break;
                    }
                    nextValidIdx++;
                }

                if (i4 < this.eldGraphDataList.size() - 1) {
                    drawStatusLine(segStart, segEnd, currentStatus);
                    drawSegmentTimeLabel(segStart, segEnd, currentStatus);
                    drawDot(segStart, currentStatus);
                }

                // Draw vertical transition line to NEXT duty status (if there is one)
                if (nextValidIdx < this.eldGraphDataList.size()) {
                    int nextStatus = convertEventNameToStatus(
                            ((ELDGraphData) this.eldGraphDataList.get(nextValidIdx)).getStatus());
                    if (nextStatus >= 1 && nextStatus <= 4 && nextStatus != currentStatus) {
                        changeDutyStatus(currentStatus, nextStatus, segEnd);
                    }
                }
            } else if (currentStatus == 5 || currentStatus == 6) {
                // It's an ENGINE ON/OFF event - just draw a dot at the last valid duty status
                float eventTime = ((ELDGraphData) this.eldGraphDataList.get(i4)).getTime();
                drawDot(eventTime, lastValidDutyStatus);
            }

            i4 = i12;
        }
        // Draw green dot at the very last data point if it's a duty status
        if (!this.eldGraphDataList.isEmpty()) {
            ELDGraphData lastPoint = (ELDGraphData) this.eldGraphDataList.get(this.eldGraphDataList.size() - 1);
            int lastStatus = convertEventNameToStatus(lastPoint.getStatus());
            if (lastStatus >= 1 && lastStatus <= 4) {
                drawDot(lastPoint.getTime(), lastStatus);
            } else if (lastStatus == 5 || lastStatus == 6) {
                drawDot(lastPoint.getTime(), lastValidDutyStatus);
            }
        }

        // Get current time in hours (24-hour format)
        // long currentTimeMillis = System.currentTimeMillis();
        // Calendar calendar = Calendar.getInstance();
        // calendar.setTimeInMillis(currentTimeMillis);
        // float currentHour = calendar.get(Calendar.HOUR_OF_DAY) +
        // (calendar.get(Calendar.MINUTE) / 60.0f);

        // long currentTimeMillis = System.currentTimeMillis();
        // TimeZone pstZone = TimeZone.getTimeZone("America/Los_Angeles"); // PST/PDT
        // zone
        // Calendar calendar = Calendar.getInstance(pstZone);
        // calendar.setTimeInMillis(currentTimeMillis);
        // float currentHour = calendar.get(Calendar.HOUR_OF_DAY) +
        // (calendar.get(Calendar.MINUTE) / 60.0f);
        //
        // if (!eldGraphDataList.isEmpty()) {
        // // Get last data point
        // ELDGraphData lastData = eldGraphDataList.get(eldGraphDataList.size() - 1);
        //
        // // Convert last event to status
        // int lastStatus = convertEventNameToStatus(lastData.getStatus());
        //
        // // Draw a horizontal line from last event time to current time
        // drawStatusLine(lastData.getTime(), currentHour, lastStatus);
        // }

    }

    private int convertEventNameToStatus(String value) {
        if (value == null)
            return 0;
        String v = value.trim().toLowerCase();
        if ("off".equals(v))
            return 1;
        if ("sb".equals(v))
            return 2;
        if ("d".equals(v) || "dr".equals(v))
            return 3;
        if ("on".equals(v))
            return 4;
        if ("eng_on".equals(v) || "e_on".equals(v) || "power_on".equals(v))
            return 5;
        if ("eng_off".equals(v) || "e_off".equals(v) || "power_off".equals(v))
            return 6;
        return 0;
    }
}
