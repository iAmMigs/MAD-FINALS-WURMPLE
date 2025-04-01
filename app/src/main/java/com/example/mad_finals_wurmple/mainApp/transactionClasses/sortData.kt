package com.example.mad_finals_wurmple.mainApp.transactionClasses


object SortData {

    fun <T, R : Comparable<R>> insertionSortBy(list: MutableList<T>, selector: (T) -> R) {
        for (i in 1 until list.size) {
            val current = list[i]
            var j = i - 1

            // Move elements that are greater than current to one position ahead
            while (j >= 0 && selector(list[j]) > selector(current)) {
                list[j + 1] = list[j]
                j--
            }

            // Place current in its correct position
            list[j + 1] = current
        }
    }

    fun <T, R : Comparable<R>> insertionSortByDescending(list: MutableList<T>, selector: (T) -> R) {
        for (i in 1 until list.size) {
            val current = list[i]
            var j = i - 1

            // Move elements that are less than current to one position ahead
            while (j >= 0 && selector(list[j]) < selector(current)) {
                list[j + 1] = list[j]
                j--
            }

            // Place current in its correct position
            list[j + 1] = current
        }
    }
}