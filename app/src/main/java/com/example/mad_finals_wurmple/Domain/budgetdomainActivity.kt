package com.example.mad_finals_wurmple.domain

import android.os.Parcel
import android.os.Parcelable

data class budgetDomainActivity(
    val title:String="",
    val price:Double=0.0,
    val percent:Double=0.0
):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readDouble(),
        parcel.readDouble()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeDouble(price)
        parcel.writeDouble(percent)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<budgetDomainActivity> {
        override fun createFromParcel(parcel: Parcel): budgetDomainActivity {
            return budgetDomainActivity(parcel)
        }

        override fun newArray(size: Int): Array<budgetDomainActivity?> {
            return arrayOfNulls(size)
        }
    }
}
