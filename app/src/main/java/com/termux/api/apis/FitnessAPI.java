package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * API for fitness/health sensor data.
 * Fix for issue #350.
 * Reads step counter, heart rate, and other fitness-related sensors.
 */
public class FitnessAPI {

    private static final String LOG_TAG = "FitnessAPI";
    private static final long DEFAULT_DURATION_MS = 5000;

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "list":
                listSensors(apiReceiver, context, intent);
                break;
            case "read":
                readSensor(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    private static void listSensors(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                if (sm == null) {
                    out.beginObject().name("error").value("SensorManager not available").endObject();
                    return;
                }

                int[] fitnessTypes = {
                        Sensor.TYPE_STEP_COUNTER,
                        Sensor.TYPE_STEP_DETECTOR,
                        Sensor.TYPE_HEART_RATE,
                        Sensor.TYPE_HEART_BEAT,
                        Sensor.TYPE_AMBIENT_TEMPERATURE,
                        Sensor.TYPE_BODY_TEMPERATURE
                };

                out.beginArray();
                for (int type : fitnessTypes) {
                    Sensor s = sm.getDefaultSensor(type);
                    if (s != null) {
                        out.beginObject();
                        out.name("type").value(sensorTypeToString(type));
                        out.name("type_id").value(type);
                        out.name("name").value(s.getName());
                        out.name("vendor").value(s.getVendor());
                        out.name("version").value(s.getVersion());
                        out.name("power_ma").value(s.getPower());
                        out.name("resolution").value(s.getResolution());
                        out.name("max_range").value(s.getMaximumRange());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            out.name("is_wake_up").value(s.isWakeUpSensor());
                            out.name("is_dynamic").value(s.isDynamicSensor());
                        }
                        out.endObject();
                    }
                }
                out.endArray();
            }
        });
    }

    private static void readSensor(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String sensorType = intent.getStringExtra("sensor_type");
        final long durationMs = intent.getLongExtra("duration_ms", DEFAULT_DURATION_MS);

        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                if (sm == null) {
                    out.beginObject().name("error").value("SensorManager not available").endObject();
                    return;
                }

                int type;
                switch (sensorType != null ? sensorType : "") {
                    case "step_counter": type = Sensor.TYPE_STEP_COUNTER; break;
                    case "step_detector": type = Sensor.TYPE_STEP_DETECTOR; break;
                    case "heart_rate": type = Sensor.TYPE_HEART_RATE; break;
                    case "heart_beat": type = Sensor.TYPE_HEART_BEAT; break;
                    case "ambient_temperature": type = Sensor.TYPE_AMBIENT_TEMPERATURE; break;
                    case "body_temperature": type = Sensor.TYPE_BODY_TEMPERATURE; break;
                    default:
                        out.beginObject().name("error").value("Unknown sensor type: " + sensorType).endObject();
                        return;
                }

                Sensor sensor = sm.getDefaultSensor(type);
                if (sensor == null) {
                    out.beginObject().name("error").value("Sensor not available: " + sensorType).endObject();
                    return;
                }

                List<float[]> readings = new ArrayList<>();
                CountDownLatch latch = new CountDownLatch(1);

                SensorEventListener listener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        readings.add(event.values.clone());
                    }
                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
                };

                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

                try {
                    Thread.sleep(durationMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    sm.unregisterListener(listener);
                }

                out.beginObject();
                out.name("sensor").value(sensorType);
                out.name("readings_count").value(readings.size());
                out.name("values");
                out.beginArray();
                for (float[] values : readings) {
                    out.beginArray();
                    for (float v : values) {
                        out.value(v);
                    }
                    out.endArray();
                }
                out.endArray();
                out.endObject();
            }
        });
    }

    private static String sensorTypeToString(int type) {
        switch (type) {
            case Sensor.TYPE_STEP_COUNTER: return "step_counter";
            case Sensor.TYPE_STEP_DETECTOR: return "step_detector";
            case Sensor.TYPE_HEART_RATE: return "heart_rate";
            case Sensor.TYPE_HEART_BEAT: return "heart_beat";
            case Sensor.TYPE_AMBIENT_TEMPERATURE: return "ambient_temperature";
            case Sensor.TYPE_BODY_TEMPERATURE: return "body_temperature";
            default: return "unknown(" + type + ")";
        }
    }
}
