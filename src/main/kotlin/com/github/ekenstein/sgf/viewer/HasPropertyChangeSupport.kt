package com.github.ekenstein.sgf.viewer

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

interface HasPropertyChangeSupport {
    fun addPropertyChangeListener(listener: PropertyChangeListener)
    fun removePropertyChangeListener(listener: PropertyChangeListener)
}