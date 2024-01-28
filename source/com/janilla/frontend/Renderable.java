package com.janilla.frontend;

import java.io.IOException;

public interface Renderable {

	Object render(Object key, RenderEngine engine) throws IOException;
}
