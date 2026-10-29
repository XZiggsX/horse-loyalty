package com.xziggsx.horseloyalty.riding.client.sense;

import com.xziggsx.horseloyalty.riding.api.Gait;

/**
 * What the behaviours get to know about the ride each tick, besides the scan.
 *
 * @param gait      the current gait
 * @param allowed   whether a behaviour may steer now: false while halted, backing up, sidestepping or bolting
 * @param steering  whether the rider is holding A or D
 * @param underfoot     the terrain under the horse, or null while it's in the air
 * @param blocksPerTick how far the gait carries the horse each tick on flat ground
 */
public record SenseState(Gait gait, boolean allowed, boolean steering, Terrain underfoot, double blocksPerTick) {
}
