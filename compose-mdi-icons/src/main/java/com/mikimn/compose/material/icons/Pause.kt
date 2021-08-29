package com.mikimn.compose.material.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

// Ma,b => moveTo(a, b)
// Ha => horizontalLineTo(a)
// Va => verticalLineTo(a)
// Z => close()
val Icons.Filled.Pause: ImageVector
    get() {
        if (pause != null) {
            return pause!!
        }
        pause = materialIcon(name = "Filled.Pause") {
            materialPath {
                // M14,19H18V5H14M6,19H10V5H6V19Z
                moveTo(14f, 19f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(14f)
                moveTo(6f, 19f)
                horizontalLineTo(10f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineTo(19f)
                close()
            }
        }
        return pause!!
    }

private var pause: ImageVector? = null
