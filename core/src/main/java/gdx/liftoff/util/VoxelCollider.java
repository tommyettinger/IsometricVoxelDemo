package gdx.liftoff.util;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.ObjectSet;

import java.util.Comparator;

/**
 * What am I even doing? Something about the separated axis theorem. I have no idea what I am doing.
 * I am riding into town on a horse walking backwards with the saddle on my head.
 * @param <T> Some type that you can get a Vector3 position from.
 */
public class VoxelCollider<T extends HasPosition3D> {
    public Array<T> entities;

    private final Vector3 tempVector3 = new Vector3();

    public final Array<T> colliding = new Array<>();

    public VoxelCollider(Array<T> colliders){
        entities = new Array<>(colliders);
    }

    private final Comparator<T> xDistance = (a, b) -> Float.compare(Math.abs(tempVector3.x - a.getPosition().x), Math.abs(tempVector3.x - b.getPosition().x));
    private final Comparator<T> yDistance = (a, b) -> Float.compare(Math.abs(tempVector3.y - a.getPosition().y), Math.abs(tempVector3.y - b.getPosition().y));
    private final Comparator<T> zDistance = (a, b) -> Float.compare(Math.abs(tempVector3.z - a.getPosition().z), Math.abs(tempVector3.z - b.getPosition().z));

    /**
     * Gets the Array of T entities that collide with the given Vector3 position.
     * If {@code collder} is a T in {@link #entities} (by identity), then the returned Array will never contain
     * that T entity.
     * @param collider a
     * @return a reused Array (which will change on the next call to this method) of all T entities that overlap with {@code collider}, not including itself.
     */
    public Array<T> collisionsWith(T collider) {
        Vector3 voxelPosition = collider.getPosition();
        tempVector3.set(voxelPosition);
        colliding.clear();
        entities.sort(xDistance);
        float c = tempVector3.x;
        for (T e : entities) {
            if(Math.abs(c - e.getPosition().x) < 1f){
                // The distance between x coordinates is close enough for them to be overlapping
                colliding.add(e);
            } else {
                // Because the array was sorted, once we have anything with too much distance, we can stop.
                break;
            }
        }
        colliding.removeValue(collider, true);
        if(colliding.isEmpty()) return colliding;
        colliding.sort(yDistance);
        c = tempVector3.y;
        for (Array.ArrayIterator<T> iterator = colliding.iterator(); iterator.hasNext(); ) {
            T e = iterator.next();
            if (Math.abs(c) - e.getPosition().y >= 1f) {
                iterator.remove();
            }
        }
        if(colliding.isEmpty()) return colliding;
        colliding.sort(zDistance);
        c = tempVector3.z;
        for (Array.ArrayIterator<T> iterator = colliding.iterator(); iterator.hasNext(); ) {
            T e = iterator.next();
            if (Math.abs(c) - e.getPosition().z >= 1f) {
                iterator.remove();
            }
        }
        return colliding;
    }
}
