package fr.dynamx.core.client.renders.mesh.shapes;

import fr.dynamx.core.client.renders.mesh.DxIndexBuffer;
import fr.dynamx.core.client.renders.mesh.GLMesh;
import fr.dynamx.core.client.renders.mesh.VertexBuffer;
import org.lwjgl.opengl.GL11;

public class GridGLMesh extends GLMesh {



    public GridGLMesh(int xLine, int zLine, float slices) {
        super(GL11.GL_LINES, (xLine + 1) * (zLine + 1) * 3);

        VertexBuffer positions = super.createPositions();

        for (int j = 0; j <= zLine; ++j) {
            for (int i = 0; i <= xLine; ++i) {
                float x = (float)i * slices;
                float y = 0;
                float z = (float)j * slices;
                positions.put(x);
                positions.put(y);
                positions.put(z);
            }
        }

        DxIndexBuffer indices = super.createIndices((xLine * zLine * 2) * 4);

        for (int j = 0; j < zLine; ++j) {
            for (int i = 0; i < xLine; ++i) {

                int row1 = j * (xLine + 1);
                int row2 = (j + 1) * (xLine + 1);

                indices.put(row1 + i);
                indices.put(row1 + i + 1);
                indices.put(row1 + i + 1);
                indices.put(row2 + i + 1);

                indices.put(row2 + i + 1);
                indices.put(row2 + i);
                indices.put(row2 + i);
                indices.put(row1 + i);
            }
        }

        indices.flip();
        positions.flip();

    }
}
