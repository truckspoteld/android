package com.eagleye.eld.utils;

import androidx.core.app.NotificationCompat;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

//@Metadata(mo38777d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\n\b\u0017\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007¢\u0006\u0002\u0010\bR\u001a\u0010\u0006\u001a\u00020\u0007X\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\t\u0010\n\"\u0004\b\u000b\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010¨\u0006\u0011"}, mo38778d2 = {"Lcom/nationwideeld/android/data/models/ELDGraphData;", "", "time", "", "status", "", "id", "", "(FLjava/lang/String;J)V", "getId", "()J", "setId", "(J)V", "getStatus", "()Ljava/lang/String;", "getTime", "()F", "FALCON1.0.1_productionRelease"}, mo38779k = 1, mo38780mv = {1, 7, 1}, mo38782xi = 48)
/* compiled from: ELDGraphData.kt */
public class ELDGraphData {
    public static final int $stable = 8;

    /* renamed from: id */
    private long id;
    private final String status;
    private  float time;

    public ELDGraphData(float f, String str, long j) {
        //Intrinsics.checkNotNullParameter(str, NotificationCompat.CATEGORY_STATUS);
        this.time = f;
        this.status = str;
        this.id = j;
    }

    /* JADX INFO: this call moved to the top of the method (can break code semantics) */
//    public /* synthetic */ ELDGraphData(float f, String str, long j, int i, DefaultConstructorMarker defaultConstructorMarker) {
//        this(f, str, (i & 4) != 0 ? -1 : j);
//    }

    public final float getTime() {
        return this.time;
    }

    public final String getStatus() {
        return this.status;
    }

    public final long getId() {
        return this.id;
    }

    public final void setId(long j) {
        this.id = j;
    }

    public final void setTime(float j) {
        this.time = j;
    }

}
