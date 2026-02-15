package com.truckspot.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.view.ViewCompat;

import java.util.ArrayList;
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

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
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
        
        this.numColumns = 24;
        this.numRows = 4;
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

     public ELDGraph(Context context, AttributeSet attributeSet, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this(context, (i & 2) != 0 ? null : attributeSet);
    }

    public final void plotGraph(List<? extends ELDGraphData> list) {
        Intrinsics.checkNotNullParameter(list, "eldGraphDataList");
        this.eldGraphDataList = list;
    }

    private final void changeDutyStatus(int i, int i2, float f) {
//        Timber.Forest forest = Timber.Forest;
//        forest.mo46574i("change duty -> " + i + ' ' + i2 + ' ' + f, new Object[0]);
        Canvas canvas2 = this.canvas;
        if (canvas2 != null) {
            canvas2.drawLine((((float) getWidth()) * f) / ((float) this.numColumns), (float) (((double) ((i * getHeight()) / this.numRows)) - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))), (f * ((float) getWidth())) / ((float) this.numColumns), (float) (((double) ((i2 * getHeight()) / this.numRows)) - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))), this.graphPlotPaint);
        }
    }

    private final void drawStatusLine(float f, float f2, int i) {
//        Timber.Forest forest = Timber.Forest;
//        forest.mo46574i("draw line -> " + f + ' ' + f2 + ' ' + i, new Object[0]);
        Canvas canvas2 = this.canvas;
        if (canvas2 != null) {
            canvas2.drawLine((f * ((float) getWidth())) / ((float) this.numColumns), (float) (((double) ((getHeight() * i) / this.numRows)) - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))), (f2 * ((float) getWidth())) / ((float) this.numColumns), (float) (((double) ((i * getHeight()) / this.numRows)) - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))), this.graphPlotPaint);
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
                canvas2.drawLine((float) ((getWidth() * i7) / this.numColumns), 0.0f, (float) ((getWidth() * i7) / this.numColumns), (float) getHeight(), this.whitePaint);
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
                            canvas2.drawLine((float) (((double) ((getWidth() * i11) / this.numColumns)) + ((((double) getWidth()) * 0.5d) / ((double) this.numColumns))), (float) ((getHeight() * i9) / this.numRows), (float) (((double) ((getWidth() * i11) / this.numColumns)) + ((((double) getWidth()) * 0.5d) / ((double) this.numColumns))), (float) (((double) ((getHeight() * i9) / this.numRows)) - ((((double) getHeight()) * 0.5d) / ((double) this.numRows))), this.whitePaint);
                        } else {
                            i2 = i8;
                        }
                        if (canvas3 != null) {
                            canvas2.drawLine((float) (((double) ((getWidth() * i11) / this.numColumns)) + ((((double) getWidth()) * 0.25d) / ((double) this.numColumns))), (float) ((getHeight() * i9) / this.numRows), (float) (((double) ((getWidth() * i11) / this.numColumns)) + ((((double) getWidth()) * 0.25d) / ((double) this.numColumns))), (float) (((double) ((getHeight() * i9) / this.numRows)) - ((((double) getHeight()) * 0.25d) / ((double) this.numRows))), this.whitePaint);
                        }
                        if (canvas3 != null) {
                            canvas2.drawLine((float) (((double) ((getWidth() * i11) / this.numColumns)) + ((((double) getWidth()) * 0.75d) / ((double) this.numColumns))), (float) ((getHeight() * i9) / this.numRows), (float) (((double) ((getWidth() * i11) / this.numColumns)) + ((((double) getWidth()) * 0.75d) / ((double) this.numColumns))), (float) (((double) ((getHeight() * i9) / this.numRows)) - ((((double) getHeight()) * 0.25d) / ((double) this.numRows))), this.whitePaint);
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
        for (Object next : this.eldGraphDataList) {
            ELDGraphData eLDGraphData = (ELDGraphData) next;
            // Include ALL duty statuses, don't exclude E_ON and E_OFF
            arrayList.add(next);
        }
        for (Object next2 : (List) arrayList) {
            int i12 = i4 + 1;
            if (i4 < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            ELDGraphData eLDGraphData2 = (ELDGraphData) next2;
            int currentStatus = convertEventNameToStatus(((ELDGraphData) this.eldGraphDataList.get(i4)).getStatus());
            if (i4 < this.eldGraphDataList.size() - 1 && currentStatus != 0) {
                drawStatusLine(((ELDGraphData) this.eldGraphDataList.get(i4)).getTime(), ((ELDGraphData) this.eldGraphDataList.get(i12)).getTime(), currentStatus);
            }
            if (i4 != 0) {
                int prevStatus = convertEventNameToStatus(((ELDGraphData) this.eldGraphDataList.get(i4 - 1)).getStatus());
                if (prevStatus != 0 && currentStatus != 0) {
                    changeDutyStatus(prevStatus, currentStatus, ((ELDGraphData) this.eldGraphDataList.get(i4)).getTime());
                }
            }
            i4 = i12;
        }
        // Get current time in hours (24-hour format)
//        long currentTimeMillis = System.currentTimeMillis();
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(currentTimeMillis);
//        float currentHour = calendar.get(Calendar.HOUR_OF_DAY) + (calendar.get(Calendar.MINUTE) / 60.0f);

//        long currentTimeMillis = System.currentTimeMillis();
//        TimeZone pstZone = TimeZone.getTimeZone("America/Los_Angeles"); // PST/PDT zone
//        Calendar calendar = Calendar.getInstance(pstZone);
//        calendar.setTimeInMillis(currentTimeMillis);
//        float currentHour = calendar.get(Calendar.HOUR_OF_DAY) + (calendar.get(Calendar.MINUTE) / 60.0f);
//
//        if (!eldGraphDataList.isEmpty()) {
//            // Get last data point
//            ELDGraphData lastData = eldGraphDataList.get(eldGraphDataList.size() - 1);
//
//            // Convert last event to status
//            int lastStatus = convertEventNameToStatus(lastData.getStatus());
//
//            // Draw a horizontal line from last event time to current time
//            drawStatusLine(lastData.getTime(), currentHour, lastStatus);
//        }

    }


    private int convertEventNameToStatus(String value) {
        if (value == null) return 0;
        String v = value.trim().toLowerCase();
        if ("off".equals(v)) return 1;
        if ("sb".equals(v)) return 2;
        if ("d".equals(v) || "dr".equals(v)) return 3;
        if ("on".equals(v)) return 4;
        return 0;
    }
}
