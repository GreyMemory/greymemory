/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.greymemory.anomaly;

import com.greymemory.evolution.Individual;

/**
 *
 * @author anton
 */
// An interface to be implemented by everyone interested in Anomaly events
public interface AnomalyConsumer {
    void OnAnomaly(Anomaly anomaly, Individual individual, DataSource data_source);
}
