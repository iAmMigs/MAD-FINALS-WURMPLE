package com.example.mad_finals_wurmple.mainApp.transactionClasses

// Necessary import for MutableList
import kotlin.collections.MutableList

// SortData class with properly typed generic methods
class SortData {
    companion object {
        // Generic method to sort a list of any type in ascending order
        fun <T, R : Comparable<R>> insertionSortBy(list: MutableList<T>, selector: (T) -> R): MutableList<T> {
            for (i in 1 until list.size) {
                val current = list[i]
                var j = i - 1

                // Move elements of list[0..i-1], that are greater than current, one position ahead
                while (j >= 0 && selector(list[j]).compareTo(selector(current)) > 0) {
                    list[j + 1] = list[j]
                    j--
                }

                list[j + 1] = current
            }
            return list
        }

        // Generic method to sort a list of any type in descending order
        fun <T, R : Comparable<R>> insertionSortByDescending(list: MutableList<T>, selector: (T) -> R): MutableList<T> {
            for (i in 1 until list.size) {
                val current = list[i]
                var j = i - 1

                // Move elements of list[0..i-1], that are less than current, one position ahead
                while (j >= 0 && selector(list[j]).compareTo(selector(current)) < 0) {
                    list[j + 1] = list[j]
                    j--
                }

                list[j + 1] = current
            }
            return list
        }
    }
}