package gdx.liftoff.util;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;

/**
 * Allows checking a group of {@code T} entities, where an entity is any type that can have a Vector3 position retrieved
 * from it via the {@link HasPosition3D} interface, for collisions with another entity or a member of its own group.
 * This doesn't currently allow getting a minimum translation vector to undo the collision, but since this only is meant
 * to check one moving entity at a time against all other entities, you can potentially refuse the movement that would
 * result in the collision by tracking the previous position and reverting to it if any collision occurs.
 * @param <T> Any type that you can get a Vector3 position from; must implement HasPosition3D, often {@link gdx.liftoff.game.Mover}
 */
public class VoxelCollider<T extends HasPosition3D> {

    /**
     * The Array of all HasPosition3D entities that can enter a collision.
     */
    public Array<T> entities;

    /**
     * Temporary Vector3 to avoid allocating these all the time,
     */
    private final Vector3 tempVector3 = new Vector3();

    /**
     * Stores the most recent Array of colliding entities, which can be empty if there are no overlaps.
     */
    public final Array<T> colliding = new Array<>();

    /**
     * Creates a VoxelCollider with an empty entities Array.
     */
    public VoxelCollider() {
        entities = new Array<>(16);
    }

    /**
     * Creates a VoxelCollider with a copy of the given Array of entities.
     * @param colliders an Array of entities that will be copied
     */
    public VoxelCollider(Array<T> colliders){
        entities = new Array<>(colliders);
    }

    /**
     * Gets the Array of T entities that collide with the given Vector3 position.
     * If {@code collder} is a T in {@link #entities} (by identity), then the returned Array will never contain
     * that T entity.
     * <br>
     * This uses the Separated Axis Theorem to limit how much work must be done to process entities. It takes the list
     * of entities and adds any candidates for a collision to {@link #colliding}, determined at first by being close
     * enough on the x-axis. Once it has whatever entities are close on the x-axis, it checks only those to see if they
     * are close on y, removing any that aren't, repeating that step for entities close on z, and finally leaving only
     * the entities that have been close on x, y, and z in {@link #colliding}, which this reuses.
     * @param collider an entity to check for collision; will never be considered self-colliding, and may be in entities
     * @return a reused Array (which will change on the next call to this method) of all T entities that overlap with {@code collider}, not including itself.
     */
    public Array<T> collisionsWith(T collider) {
        Vector3 voxelPosition = collider.getPosition();
        // We reuse the position in tempVector3, which belongs to the VoxelCollider so the comparator can use it.
        tempVector3.set(voxelPosition);
        // The reused colliding Array must be cleared to avoid the last calculation remaining here.
        colliding.clear();
        float c = tempVector3.x;
        for (T e : entities) {
            if(Math.abs(c - e.getPosition().x) < 1f){
                // The distance between x coordinates is close enough for them to be overlapping
                colliding.add(e);
            }
        }
        // Remove the collider we are checking by identity, so it can't collide with itself.
        colliding.removeValue(collider, true);
        // If there's nothing else to remove, we're done!
        if(colliding.isEmpty()) return colliding;
        // Do these steps again on y, but without sorting now. There should be very few entities to check now.
        c = tempVector3.y;
        // We have to get an Iterator for the Array so we can remove during iteration.
        for (Array.ArrayIterator<T> iterator = colliding.iterator(); iterator.hasNext(); ) {
            T e = iterator.next();
            if (Math.abs(c - e.getPosition().y) >= 1f) {
                iterator.remove();
            }
        }
        // If there's nothing else to remove, we're done!
        if(colliding.isEmpty()) return colliding;
        // Do the previous steps for y, but on z this time.
        c = tempVector3.z;
        for (Array.ArrayIterator<T> iterator = colliding.iterator(); iterator.hasNext(); ) {
            T e = iterator.next();
            if (Math.abs(c - e.getPosition().z) >= 1f) {
                iterator.remove();
            }
        }
        // Any entities left in the Array will overlap on x, y, and z, meaning they collide.
        return colliding;
    }
}
