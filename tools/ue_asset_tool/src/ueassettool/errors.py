class UEAssetError(Exception):
    """Base error for a package that cannot be decoded safely."""


class BoundsError(UEAssetError):
    """A read would leave the declared file or object boundary."""


class FormatError(UEAssetError):
    """Bytes violate a known Unreal serialization invariant."""


class UnsupportedError(UEAssetError):
    """The input uses a feature this build does not claim to decode."""

