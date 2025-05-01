package gdx.liftoff.util;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

/**
 * What am I even doing? Something about the separated axis theorem. I have no idea what I am doing.
 * I am riding into town on a horse walking backwards with the saddle on my head.
 * @param <T> Some type that you can get a Vector3 position from.
 */
public class VoxelCollider<T extends HasPosition3D> {
    public Array<T> entities;
    protected FloatArray xPositions = new FloatArray();
    protected FloatArray yPositions = new FloatArray();
    protected FloatArray zPositions = new FloatArray();

    private final Vector3 tempVector3 = new Vector3();

    public VoxelCollider(Array<T> colliders){
        entities = new Array<>(colliders);
        for(T e : entities){
            Vector3 v = e.getPosition();
            xPositions.add(v.x);
            yPositions.add(v.y);
            zPositions.add(v.z);
        }
    }
}
