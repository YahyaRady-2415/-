package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

data class CompassReading(
    val azimuthDegrees: Float,
    val hasSensor: Boolean,
)

class CompassSensorManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observeAzimuth(): Flow<CompassReading> = callbackFlow {
        val rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotSensor == null && (accelSensor == null || magSensor == null)) {
            trySend(CompassReading(azimuthDegrees = 0f, hasSensor = false))
            awaitClose {}
            return@callbackFlow
        }

        val accelReading = FloatArray(3)
        val magReading = FloatArray(3)
        var hasAccel = false
        var hasMag = false
        var smoothedAzimuth = 0f
        var initialized = false

        fun emitSmoothed(rawDegrees: Float) {
            val normalized = ((rawDegrees % 360f) + 360f) % 360f
            if (!initialized) {
                smoothedAzimuth = normalized
                initialized = true
            } else {
                var delta = normalized - smoothedAzimuth
                if (delta > 180f) delta -= 360f
                if (delta < -180f) delta += 360f
                if (abs(delta) > 0.3f) {
                    smoothedAzimuth = ((smoothedAzimuth + delta * 0.18f) % 360f + 360f) % 360f
                }
            }
            trySend(CompassReading(azimuthDegrees = smoothedAzimuth, hasSensor = true))
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotMatrix, orientation)
                        val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        emitSmoothed(deg)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, accelReading, 0, 3)
                        hasAccel = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magReading, 0, 3)
                        hasMag = true
                    }
                }
                if (rotSensor == null && hasAccel && hasMag) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, accelReading, magReading)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        emitSmoothed(deg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotSensor != null) {
            sensorManager.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
