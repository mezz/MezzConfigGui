package net.mezzdev.config.gui.util;

/**
 * Immutable integer rectangle used by config screen layout and hit testing.
 */
public record ImmutableRect2i(
	int x,
	int y,
	int width,
	int height
) {
	public static final ImmutableRect2i EMPTY = new ImmutableRect2i(0, 0, 0, 0);

	public ImmutableRect2i {
		if (width < 0) {
			throw new IllegalArgumentException("width must be >= 0");
		}
		if (height < 0) {
			throw new IllegalArgumentException("height must be >= 0");
		}
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean isEmpty() {
		return width == 0 || height == 0;
	}

	public boolean contains(double x, double y) {
		return x >= this.x &&
			y >= this.y &&
			x < this.x + this.width &&
			y < this.y + this.height;
	}

	public boolean intersects(ImmutableRect2i rect) {
		if (isEmpty() || rect.isEmpty()) {
			return false;
		}
		return rect.getX() + rect.getWidth() > x &&
			rect.getY() + rect.getHeight() > y &&
			rect.getX() < x + width &&
			rect.getY() < y + height;
	}

	public ImmutableRect2i insetBy(int amount) {
		if (amount == 0) {
			return this;
		}
		amount = Math.min(amount, width / 2);
		amount = Math.min(amount, height / 2);
		int doubleAmount = Math.multiplyExact(amount, 2);
		return new ImmutableRect2i(
			Math.addExact(x, amount),
			Math.addExact(y, amount),
			Math.subtractExact(width, doubleAmount),
			Math.subtractExact(height, doubleAmount)
		);
	}

	public ImmutableRect2i cropRight(int amount) {
		if (amount == 0) {
			return this;
		}
		amount = Math.min(amount, width);
		return new ImmutableRect2i(
			x,
			y,
			Math.subtractExact(width, amount),
			height
		);
	}

	public ImmutableRect2i cropLeft(int amount) {
		if (amount == 0) {
			return this;
		}
		amount = Math.min(amount, width);
		return new ImmutableRect2i(
			Math.addExact(x, amount),
			y,
			Math.subtractExact(width, amount),
			height
		);
	}

	public ImmutableRect2i cropBottom(int amount) {
		if (amount == 0) {
			return this;
		}
		amount = Math.min(amount, height);
		return new ImmutableRect2i(
			x,
			y,
			width,
			Math.subtractExact(height, amount)
		);
	}

	public ImmutableRect2i cropTop(int amount) {
		if (amount == 0) {
			return this;
		}
		amount = Math.min(amount, height);
		return new ImmutableRect2i(
			x,
			Math.addExact(y, amount),
			width,
			Math.subtractExact(height, amount)
		);
	}

	public ImmutableRect2i keepTop(int amount) {
		if (amount >= height) {
			return this;
		}
		return new ImmutableRect2i(x, y, width, amount);
	}

	public ImmutableRect2i keepBottom(int amount) {
		if (amount >= height) {
			return this;
		}
		int cropAmount = Math.subtractExact(height, amount);
		return new ImmutableRect2i(
			x,
			Math.addExact(y, cropAmount),
			width,
			amount
		);
	}

	public ImmutableRect2i keepRight(int amount) {
		if (amount >= width) {
			return this;
		}
		int cropAmount = Math.subtractExact(width, amount);
		return new ImmutableRect2i(
			Math.addExact(x, cropAmount),
			y,
			amount,
			height
		);
	}

	public ImmutableRect2i keepLeft(int amount) {
		if (amount >= width) {
			return this;
		}
		return new ImmutableRect2i(x, y, amount, height);
	}
}
