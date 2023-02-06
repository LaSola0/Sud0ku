# test


class A:
    def __init__(self, value):
        self.value = value

    def __sub__(self, other):
        if isinstance(other, int):
            return 2 * othe