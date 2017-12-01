package ro.traveldevel.obd;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.DistanceMILOnCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;

import java.util.ArrayList;

public final class ObdConfig {

    public static ArrayList<ObdCommand> getCommands() {

        ArrayList<ObdCommand> cmds = new ArrayList<>();

        // Control
        cmds.add(new DistanceMILOnCommand());
        cmds.add(new VinCommand());

        // Engine
        cmds.add(new LoadCommand());
        cmds.add(new RPMCommand());
        cmds.add(new RuntimeCommand());
        cmds.add(new MassAirFlowCommand());
        cmds.add(new ThrottlePositionCommand());

        // Temperature
        cmds.add(new AirIntakeTemperatureCommand());
        cmds.add(new AmbientAirTemperatureCommand());
        cmds.add(new EngineCoolantTemperatureCommand());

        // Misc
        cmds.add(new SpeedCommand());

        return cmds;
    }

}
