/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jzelda.solar.console;

import java.util.Optional;

/**
 *
 * @author engin
 */
public interface ICommand {
    public void exec(Optional<String[]> args);
}
