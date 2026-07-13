import unittest

from ueassettool.hashes import blake3_digest


class Blake3Tests(unittest.TestCase):
    def test_official_empty_input_vector(self) -> None:
        self.assertEqual(
            blake3_digest(b"").hex(),
            "af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262",
        )

    def test_stream_crosses_chunk_tree_boundary(self) -> None:
        self.assertEqual(
            blake3_digest(b"a" * 1025).hex(),
            "c59d2e12583df14d951e757a42f1734d355c8c5b1db6b6a33ab2bfabeed40c7d",
        )
        self.assertEqual(
            blake3_digest(b"a" * 5000).hex(),
            "09d0d29a5f2dc69dff0809823ca867836c3a3cfb00e12df06d92e3f0f70629e9",
        )


if __name__ == "__main__":
    unittest.main()
