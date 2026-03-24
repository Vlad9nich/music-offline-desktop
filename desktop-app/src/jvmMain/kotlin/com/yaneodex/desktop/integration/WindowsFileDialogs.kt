package com.yaneodex.desktop.integration

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

object WindowsFileDialogs {
    fun pickImageFiles(): List<File> {
        val dialog = FileDialog(null as Frame?, "Choose screenshots", FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.isVisible = true
        return dialog.files?.filter { it.isFile } ?: emptyList()
    }

    fun pickFolders(): List<File> {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isMultiSelectionEnabled = true
        }
        val result = chooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFiles?.filter { it.isDirectory } ?: emptyList()
        } else {
            emptyList()
        }
    }
}
