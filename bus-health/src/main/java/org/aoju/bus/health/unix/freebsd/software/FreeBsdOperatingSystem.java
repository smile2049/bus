/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2020 aoju.org OSHI and other contributors.                 *
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
package org.aoju.bus.health.unix.freebsd.software;

import org.aoju.bus.core.annotation.ThreadSafe;
import org.aoju.bus.core.lang.Normal;
import org.aoju.bus.core.lang.RegEx;
import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.health.Builder;
import org.aoju.bus.health.Executor;
import org.aoju.bus.health.builtin.software.*;
import org.aoju.bus.health.unix.freebsd.BsdSysctlKit;
import org.aoju.bus.health.unix.freebsd.FreeBsdLibc;
import org.aoju.bus.health.unix.freebsd.drivers.Who;
import org.aoju.bus.logger.Logger;

import java.io.File;
import java.util.*;

/**
 * FreeBSD is a free and open-source Unix-like operating system descended from
 * the Berkeley Software Distribution (BSD), which was based on Research Unix.
 * The first version of FreeBSD was released in 1993. In 2005, FreeBSD was the
 * most popular open-source BSD operating system, accounting for more than
 * three-quarters of all installed simply, permissively licensed BSD systems.
 */
@ThreadSafe
public class FreeBsdOperatingSystem extends AbstractOperatingSystem {

    private static final long BOOTTIME = querySystemBootTime();

    private static List<OSProcess> getProcessListFromPS(int pid) {
        List<OSProcess> procs = new ArrayList<>();
        String psCommand = "ps -awwxo state,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etimes,systime,time,comm,args";
        if (pid >= 0) {
            psCommand += " -p " + pid;
        }
        List<String> procList = Executor.runNative(psCommand);
        if (procList.isEmpty() || procList.size() < 2) {
            return procs;
        }
        // remove header row
        procList.remove(0);
        // Fill list
        for (String proc : procList) {
            String[] split = RegEx.SPACES.split(proc.trim(), 16);
            // Elements should match ps command order
            if (split.length == 16) {
                procs.add(new FreeBsdOSProcess(pid < 0 ? Builder.parseIntOrDefault(split[1], 0) : pid, split));
            }
        }
        return procs;
    }

    private static long querySystemBootTime() {
        FreeBsdLibc.Timeval tv = new FreeBsdLibc.Timeval();
        if (!BsdSysctlKit.sysctl("kern.boottime", tv) || tv.tv_sec == 0) {
            // Usually this works. If it doesn't, fall back to text parsing.
            // Boot time will be the first consecutive string of digits.
            return Builder.parseLongOrDefault(
                    Executor.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                    System.currentTimeMillis() / 1000);
        }
        // tv now points to a 128-bit timeval structure for boot time.
        // First 8 bytes are seconds, second 8 bytes are microseconds (we ignore)
        return tv.tv_sec;
    }

    @Override
    public String queryManufacturer() {
        return "Unix/BSD";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        String family = BsdSysctlKit.sysctl("kern.ostype", "FreeBSD");

        String version = BsdSysctlKit.sysctl("kern.osrelease", Normal.EMPTY);
        String versionInfo = BsdSysctlKit.sysctl("kern.version", Normal.EMPTY);
        String buildNumber = versionInfo.split(Symbol.COLON)[0].replace(family, Normal.EMPTY).replace(version, Normal.EMPTY).trim();

        return new FamilyVersionInfo(family, new OperatingSystem.OSVersionInfo(version, null, buildNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && Executor.getFirstAnswer("uname -m").indexOf("64") == -1) {
            return jvmBitness;
        }
        return 64;
    }

    @Override
    protected boolean queryElevated() {
        return System.getenv("SUDO_COMMAND") != null;
    }

    @Override
    public FileSystem getFileSystem() {
        return new FreeBsdFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new FreeBsdInternetProtocolStats();
    }

    @Override
    public List<OSSession> getSessions() {
        return Collections.unmodifiableList(USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent());
    }

    @Override
    public List<OSProcess> getProcesses(int limit, OperatingSystem.ProcessSort sort) {
        List<OSProcess> procs = getProcessListFromPS(-1);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return Collections.unmodifiableList(sorted);
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromPS(pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public List<OSProcess> getChildProcesses(int parentPid, int limit, OperatingSystem.ProcessSort sort) {
        List<OSProcess> procs = new ArrayList<>();
        for (OSProcess p : getProcesses(0, null)) {
            if (p.getParentProcessID() == parentPid) {
                procs.add(p);
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return Collections.unmodifiableList(sorted);
    }

    @Override
    public int getProcessId() {
        return FreeBsdLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        List<String> procList = Executor.runNative("ps -axo pid");
        if (!procList.isEmpty()) {
            // Subtract 1 for header
            return procList.size() - 1;
        }
        return 0;
    }

    @Override
    public int getThreadCount() {
        int threads = 0;
        for (String proc : Executor.runNative("ps -axo nlwp")) {
            threads += Builder.parseIntOrDefault(proc.trim(), 0);
        }
        return threads;
    }

    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - BOOTTIME;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new FreeBsdNetworkParams();
    }

    @Override
    public OSService[] getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, 0, OperatingSystem.ProcessSort.PID)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), OSService.State.RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        // Get Directories for stopped services
        File dir = new File("/etc/rc.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File f : listFiles) {
                String name = f.getName();
                if (!running.contains(name)) {
                    OSService s = new OSService(name, 0, OSService.State.STOPPED);
                    services.add(s);
                }
            }
        } else {
            Logger.error("Directory: /etc/init does not exist");
        }
        return services.toArray(new OSService[0]);
    }

}