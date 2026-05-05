#pragma once
#include <cmath>
#include <cstdint>

namespace solum {

struct Mat4 {
    float m[16];

    static Mat4 identity() {
        Mat4 r{};
        r.m[0] = 1.0f;
        r.m[5] = 1.0f;
        r.m[10] = 1.0f;
        r.m[15] = 1.0f;
        return r;
    }

    static Mat4 perspective(float fovyRadians, float aspect, float zNear, float zFar) {
        Mat4 r{};
        const float f = 1.0f / std::tan(fovyRadians * 0.5f);
        r.m[0] = f / aspect;
        r.m[5] = -f;
        r.m[10] = zFar / (zNear - zFar);
        r.m[11] = -1.0f;
        r.m[14] = (zNear * zFar) / (zNear - zFar);
        return r;
    }

    static Mat4 translation(float x, float y, float z) {
        Mat4 r = identity();
        r.m[12] = x;
        r.m[13] = y;
        r.m[14] = z;
        return r;
    }

    static Mat4 rotationX(float radians) {
        Mat4 r = identity();
        const float c = std::cos(radians);
        const float s = std::sin(radians);
        r.m[5] = c;
        r.m[6] = s;
        r.m[9] = -s;
        r.m[10] = c;
        return r;
    }

    static Mat4 rotationY(float radians) {
        Mat4 r = identity();
        const float c = std::cos(radians);
        const float s = std::sin(radians);
        r.m[0] = c;
        r.m[2] = -s;
        r.m[8] = s;
        r.m[10] = c;
        return r;
    }
};

inline Mat4 multiply(const Mat4& a, const Mat4& b) {
    Mat4 r{};
    for (int col = 0; col < 4; ++col) {
        for (int row = 0; row < 4; ++row) {
            r.m[col * 4 + row] =
                a.m[0 * 4 + row] * b.m[col * 4 + 0] +
                a.m[1 * 4 + row] * b.m[col * 4 + 1] +
                a.m[2 * 4 + row] * b.m[col * 4 + 2] +
                a.m[3 * 4 + row] * b.m[col * 4 + 3];
        }
    }
    return r;
}

inline Mat4 makeValidationObjectMvp(uint32_t width, uint32_t height) {
    const float aspect = height == 0 ? 1.0f : (float)width / (float)height;
    Mat4 proj = Mat4::perspective(60.0f * 3.1415926535f / 180.0f, aspect, 0.1f, 32.0f);
    Mat4 view = Mat4::translation(0.0f, 0.0f, -4.0f);
    Mat4 rot = multiply(Mat4::rotationY(0.65f), Mat4::rotationX(-0.35f));
    return multiply(proj, multiply(view, rot));
}

} // namespace solum
