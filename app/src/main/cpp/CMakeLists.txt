cmake_minimum_required(VERSION 3.22.1)

project(bluetooth_hidden_bridge LANGUAGES CXX)

add_library(
    bluetooth_hidden_bridge
    SHARED
    bluetooth_hidden_bridge.cpp
)

find_library(log-lib log)

target_link_libraries(
    bluetooth_hidden_bridge
    ${log-lib}
)
