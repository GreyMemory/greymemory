# GreyMemory
Floating point Sparse Distributed Memory 

## Description

GreyMemory is a new implementation of [sparce distributed memory](https://en.wikipedia.org/wiki/Sparse_distributed_memory). 

## What is new in GreyMemory?

We implemented a set of completely new features:

* Floating point input data   
* Extended storage enabling storing long time series  
* We define and use resolution for input data, rather than a range of values. No more ranges for input channels.  
* We use hyper-rectangle to allocate hard locations. In the original SDM an activation radius is used.
* We dynamically allocate hard locations on the fly
* We introduced a notion of META data. Meta data might be used for instance to retrieve an index of a long time series.
* We use a sliding window to predict store time series. It means greymemory is able to predict the next sample in the future on the fly.

## How to install

## Examples

## Future development




