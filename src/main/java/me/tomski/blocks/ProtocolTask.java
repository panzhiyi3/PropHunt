package me.tomski.blocks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.EnumWrappers;
import me.tomski.prophunt.GameManager;
import me.tomski.prophunt.PropHunt;
import me.tomski.utils.SolidBlockTracker;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class ProtocolTask implements Listener {

    private PropHunt plugin;

    public ProtocolTask(PropHunt plugin) {
        this.plugin = plugin;
    }


    public void initProtocol() {
        PropHunt.protocolManager.getAsynchronousManager().
                registerAsyncHandler(new PacketAdapter(plugin, PacketType.Play.Client.BLOCK_PLACE) {

                    @Override
                    public void onPacketSending(PacketEvent event) {
                        System.out.println("sent packet " + event.getPacketType());
                    }

                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        if (GameManager.hiders.contains(event.getPlayer().getName())) {
                            int x = event.getPacket().getIntegers().read(0);
                            int y = event.getPacket().getIntegers().read(1);
                            int z = event.getPacket().getIntegers().read(2);
                            for (SolidBlock s : SolidBlockTracker.solidBlocks.values()) {
                                if (s.loc.getBlockX() == x) {
                                    if (s.loc.getBlockY() == y) {
                                        if (s.loc.getBlockZ() == z) {
                                            event.setCancelled(true);
                                        }
                                    }
                                }
                            }
                        }
                        if (GameManager.seekers.contains(event.getPlayer().getName())) {
                            int x = event.getPacket().getIntegers().read(0);
                            int y = event.getPacket().getIntegers().read(1);
                            int z = event.getPacket().getIntegers().read(2);
                            for (SolidBlock s : SolidBlockTracker.solidBlocks.values()) {
                                if (s.loc.getBlockX() == x) {
                                    if (s.loc.getBlockY() == y) {
                                        if (s.loc.getBlockZ() == z) {
                                            event.setCancelled(true);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Get entity trackers is not thread safe
                }

                ).

                syncStart();


        PropHunt.protocolManager.getAsynchronousManager().

                registerAsyncHandler(new PacketAdapter(plugin, PacketType.Play.Client.ARM_ANIMATION) {

                    @Override
                    public void onPacketSending(PacketEvent event) {
                        System.out.println("sent packet " + event.getPacketType());
                    }

                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        final int ATTACK_REACH = 3;

                        Player observer = event.getPlayer();
                        if (observer.isDead()) {
                            return;
                        }
                        Location observerPos = observer.getEyeLocation();
                        Vector3D observerDir = new Vector3D(observerPos.getDirection());

                        Vector3D observerStart = new Vector3D(observerPos);
                        Vector3D observerEnd = observerStart.add(observerDir.multiply(ATTACK_REACH));

                        Player hit = null;

                        // Get nearby entities
                        try {
                            for (Player target : PropHunt.protocolManager.getEntityTrackers(observer)) {
                                // No need to simulate an attack if the
                                // player is already visible
                                if (!observer.canSee(target)) {
                                    // Bounding box of the given player
                                    Vector3D targetPos = new Vector3D(target.getLocation());
                                    Vector3D minimum = targetPos.add(-0.5, 0, -0.5);
                                    Vector3D maximum = targetPos.add(0.5, 1.67, 0.5);

                                    if (hasIntersection(observerStart,
                                            observerEnd, minimum, maximum)) {
                                        if (hit == null
                                                || hit.getLocation().distanceSquared(observerPos) > target.getLocation().distanceSquared(observerPos)) {
                                            hit = target;
                                        }
                                    }
                                }
                            }
                        } catch (FieldAccessException ex) {
                            // Player is dead
                        }

                        // Simulate a hit against the closest player
                        if (hit != null) {
                            PacketContainer useEntity = PropHunt.protocolManager.createPacket(PacketType.Play.Client.USE_ENTITY);
                            useEntity.getIntegers().write(0, hit.getEntityId());
                            useEntity.getEntityUseActions().write(0, EnumWrappers.EntityUseAction.ATTACK);
                            try {
                                PropHunt.protocolManager.recieveClientPacket(event.getPlayer(), useEntity);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }

                    // Get entity trackers is not thread safe
                }

                ).

                syncStart();
    }

    private boolean hasIntersection(Vector3D p1, Vector3D p2, Vector3D min,
                                    Vector3D max) {
        final double epsilon = 0.0001f;

        Vector3D d = p2.subtract(p1).multiply(0.5);
        Vector3D e = max.subtract(min).multiply(0.5);
        Vector3D c = p1.add(d).subtract(min.add(max).multiply(0.5));
        Vector3D ad = d.abs();

        if (Math.abs(c.x) > e.x + ad.x)
            return false;
        if (Math.abs(c.y) > e.y + ad.y)
            return false;
        if (Math.abs(c.z) > e.z + ad.z)
            return false;

        if (Math.abs(d.y * c.z - d.z * c.y) > e.y * ad.z + e.z * ad.y + epsilon)
            return false;
        if (Math.abs(d.z * c.x - d.x * c.z) > e.z * ad.x + e.x * ad.z + epsilon)
            return false;
        if (Math.abs(d.x * c.y - d.y * c.x) > e.x * ad.y + e.y * ad.x + epsilon)
            return false;

        return true;
    }

    private void toggleVisibilityNative(Player observer, Player target) {
        if (observer.canSee(target)) {
            observer.hidePlayer(target);
        } else {
            observer.showPlayer(target);
        }
    }
}
