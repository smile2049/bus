/*********************************************************************************
 *                                                                               *
 * The MIT License                                                               *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 ********************************************************************************/
package org.aoju.bus.health.hardware;

import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Systemd;
import org.aoju.bus.health.hardware.linux.LinuxPowerSource;
import org.aoju.bus.health.hardware.mac.MacPowerSource;
import org.aoju.bus.health.hardware.unix.freebsd.FreeBsdPowerSource;
import org.aoju.bus.health.hardware.unix.solaris.SolarisPowerSource;
import org.aoju.bus.health.hardware.windows.WindowsPowerSource;

import java.time.LocalDate;

/**
 * A Power Source
 *
 * @author Kimi Liu
 * @version 5.6.8
 * @since JDK 1.8+
 */
public abstract class AbstractPowerSource implements PowerSource {

    private String name;
    private String deviceName;
    private double remainingCapacityPercent;
    private double timeRemainingEstimated;
    private double timeRemainingInstant;
    private double powerUsageRate;
    private double voltage;
    private double amperage;
    private boolean powerOnLine;
    private boolean charging;
    private boolean discharging;
    private CapacityUnits capacityUnits;
    private int currentCapacity;
    private int maxCapacity;
    private int designCapacity;
    private int cycleCount;
    private String chemistry;
    private LocalDate manufactureDate;
    private String manufacturer;
    private String serialNumber;
    private double temperature;

    public AbstractPowerSource(String name, String deviceName, double remainingCapacityPercent,
                               double timeRemainingEstimated, double timeRemainingInstant, double powerUsageRate, double voltage,
                               double amperage, boolean powerOnLine, boolean charging, boolean discharging, CapacityUnits capacityUnits,
                               int currentCapacity, int maxCapacity, int designCapacity, int cycleCount, String chemistry,
                               LocalDate manufactureDate, String manufacturer, String serialNumber, double temperature) {
        super();
        this.name = name;
        this.deviceName = deviceName;
        this.remainingCapacityPercent = remainingCapacityPercent;
        this.timeRemainingEstimated = timeRemainingEstimated;
        this.timeRemainingInstant = timeRemainingInstant;
        this.powerUsageRate = powerUsageRate;
        this.voltage = voltage;
        this.amperage = amperage;
        this.powerOnLine = powerOnLine;
        this.charging = charging;
        this.discharging = discharging;
        this.capacityUnits = capacityUnits;
        this.currentCapacity = currentCapacity;
        this.maxCapacity = maxCapacity;
        this.designCapacity = designCapacity;
        this.cycleCount = cycleCount;
        this.chemistry = chemistry;
        this.manufactureDate = manufactureDate;
        this.manufacturer = manufacturer;
        this.serialNumber = serialNumber;
        this.temperature = temperature;
    }

    private static PowerSource[] getPowerSources() {
        switch (Systemd.getCurrentPlatform()) {
            case WINDOWS:
                return WindowsPowerSource.getPowerSources();
            case MACOSX:
                return MacPowerSource.getPowerSources();
            case LINUX:
                return LinuxPowerSource.getPowerSources();
            case SOLARIS:
                return SolarisPowerSource.getPowerSources();
            case FREEBSD:
                return FreeBsdPowerSource.getPowerSources();
            default:
                throw new UnsupportedOperationException("Operating system not supported");
        }
    }

    /**
     * Estimated time remaining on power source, formatted as HH:mm
     *
     * @return formatted String of time remaining
     */
    private static String formatTimeRemaining(double timeInSeconds) {
        String formattedTimeRemaining;
        if (timeInSeconds < -1.5) {
            formattedTimeRemaining = "Charging";
        } else if (timeInSeconds < 0) {
            formattedTimeRemaining = "Unknown";
        } else {
            int hours = (int) (timeInSeconds / 3600);
            int minutes = (int) (timeInSeconds % 3600 / 60);
            formattedTimeRemaining = String.format("%d:%02d", hours, minutes);
        }
        return formattedTimeRemaining;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDeviceName() {
        return this.deviceName;
    }

    @Override
    public double getRemainingCapacity() {
        return getRemainingCapacityPercent();
    }

    @Override
    public double getRemainingCapacityPercent() {
        return this.remainingCapacityPercent;
    }

    @Override
    public double getTimeRemaining() {
        return getTimeRemainingEstimated();
    }

    @Override
    public double getTimeRemainingEstimated() {
        return this.timeRemainingEstimated;
    }

    @Override
    public double getTimeRemainingInstant() {
        return this.timeRemainingInstant;
    }

    @Override
    public double getPowerUsageRate() {
        return this.powerUsageRate;
    }

    @Override
    public double getVoltage() {
        return this.voltage;
    }

    @Override
    public double getAmperage() {
        return this.amperage;
    }

    @Override
    public boolean isPowerOnLine() {
        return this.powerOnLine;
    }

    @Override
    public boolean isCharging() {
        return this.charging;
    }

    @Override
    public boolean isDischarging() {
        return this.discharging;
    }

    @Override
    public CapacityUnits getCapacityUnits() {
        return this.capacityUnits;
    }

    @Override
    public int getCurrentCapacity() {
        return this.currentCapacity;
    }

    @Override
    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    @Override
    public int getDesignCapacity() {
        return this.designCapacity;
    }

    @Override
    public int getCycleCount() {
        return this.cycleCount;
    }

    @Override
    public String getChemistry() {
        return this.chemistry;
    }

    @Override
    public LocalDate getManufactureDate() {
        return this.manufactureDate;
    }

    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    @Override
    public double getTemperature() {
        return this.temperature;
    }

    @Override
    public boolean updateAttributes() {
        PowerSource[] psArr = getPowerSources();
        for (PowerSource ps : psArr) {
            if (ps.getName().equals(this.name)) {
                this.name = ps.getName();
                this.deviceName = ps.getDeviceName();
                this.remainingCapacityPercent = ps.getRemainingCapacityPercent();
                this.timeRemainingEstimated = ps.getTimeRemainingEstimated();
                this.timeRemainingInstant = ps.getTimeRemainingInstant();
                this.powerUsageRate = ps.getPowerUsageRate();
                this.voltage = ps.getVoltage();
                this.amperage = ps.getAmperage();
                this.powerOnLine = ps.isPowerOnLine();
                this.charging = ps.isCharging();
                this.discharging = ps.isDischarging();
                this.capacityUnits = ps.getCapacityUnits();
                this.currentCapacity = ps.getCurrentCapacity();
                this.maxCapacity = ps.getMaxCapacity();
                this.designCapacity = ps.getDesignCapacity();
                this.cycleCount = ps.getCycleCount();
                this.chemistry = ps.getChemistry();
                this.manufactureDate = ps.getManufactureDate();
                this.manufacturer = ps.getManufacturer();
                this.serialNumber = ps.getSerialNumber();
                this.temperature = ps.getTemperature();
                return true;
            }
        }
        // Didn't find this battery
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Device Name: ").append(getDeviceName()).append(Symbol.NEWLINE);
        sb.append("RemainingCapacityPercent: ").append(getRemainingCapacityPercent() * 100).append("%, ");
        sb.append("Time Remaining: ").append(formatTimeRemaining(getTimeRemainingEstimated())).append(", ");
        sb.append("Time Remaining Instant: ").append(formatTimeRemaining(getTimeRemainingInstant())).append(Symbol.NEWLINE);
        sb.append("Power Usage Rate: ").append(getPowerUsageRate()).append("mW, ");
        sb.append("Voltage: ").append(getVoltage()).append("V, ");
        sb.append("Amperage: ").append(getAmperage()).append("mA,\n ");
        sb.append("Power OnLine: ").append(isPowerOnLine()).append(", ");
        sb.append("Charging: ").append(isCharging()).append(", ");
        sb.append("Discharging: ").append(isDischarging()).append(Symbol.NEWLINE);
        sb.append("Capacity Units: ").append(getCapacityUnits()).append(", ");
        sb.append("Current Capacity: ").append(getCurrentCapacity()).append(", ");
        sb.append("Max Capacity: ").append(getMaxCapacity()).append(", ");
        sb.append("Design Capacity: ").append(getDesignCapacity()).append(Symbol.NEWLINE);
        sb.append("Cycle Count: ").append(getCycleCount()).append(", ");
        sb.append("Chemistry: ").append(getChemistry()).append(", ");
        sb.append("Manufacture Date: ").append(getManufactureDate() != null ? getManufactureDate() : Builder.UNKNOWN)
                .append(", ");
        sb.append("Manufacturer: ").append(getManufacturer()).append(Symbol.NEWLINE);
        sb.append("SerialNumber: ").append(getSerialNumber()).append(", ");
        sb.append("Temperature: ");
        if (getTemperature() > 0) {
            sb.append(getTemperature()).append("°C");
        } else {
            sb.append(Builder.UNKNOWN);
        }
        return sb.toString();
    }
}
