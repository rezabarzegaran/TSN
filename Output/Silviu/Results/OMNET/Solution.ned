package nesting.simulations.examples;
import ned.DatarateChannel;
import nesting.node.ethernet.VlanEtherHostQ;
import nesting.node.ethernet.VlanEtherHostSched;
import nesting.node.ethernet.VlanEtherSwitchPreemptable;
network TC
{
types:
channel C extends DatarateChannel
{
delay = 0us;
datarate = 100Mbps;
}
submodules:
SW1: VlanEtherSwitchPreemptable {
gates:
ethg[6];
}
SW2: VlanEtherSwitchPreemptable {
gates:
ethg[4];
}
ES1: VlanEtherHostSched {
}
ES3: VlanEtherHostSched {
}
ES2: VlanEtherHostSched {
}
connections:
SW1.ethg[0] <--> C <--> SW2.ethg[1];
SW1.ethg[1] <--> C <--> ES2.ethg;
SW1.ethg[2] <--> C <--> ES1.ethg;
SW2.ethg[0] <--> C <--> ES3.ethg;
}
