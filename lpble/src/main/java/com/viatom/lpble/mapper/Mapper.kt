package com.viatom.lpble.mapper


interface Mapper<I, O> {
    fun map(input: I): O
}