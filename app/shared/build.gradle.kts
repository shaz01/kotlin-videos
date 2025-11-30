
plugins {
    id("convention.android.library")
    id("convention.kmp.base")
    id("convention.kmp.serialization")
}

android {
    namespace = "$ProjectId.shared"
}