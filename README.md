# TSN

This program synthesizes gate control lists (GSLs) for time-sensingitve networks (TSN).
The code is implemented in Java and uses Google OR-Tools as the solver.

Installation:
For installing the code, you may need to install the followings:
1. Eclipse
2. Java Runtime Environment (JRE)
3. Google OR Tools.

Note: There are some incompatibility issuses between different versions of JRE and Google OR-Tools.

Please cite the following paper when using the code:

The program imlements several methods for GCL synthesis:
A: Ramon method --> IEEE 802.1 Qbv gate control list synthesis using array theory encoding

  --> @inproceedings{oliver2018ieee,
  title={IEEE 802.1 Qbv gate control list synthesis using array theory encoding},
  author={Oliver, Ramon Serna and Craciunas, Silviu S and Steiner, Wilfried},
  booktitle={2018 IEEE Real-Time and Embedded Technology and Applications Symposium (RTAS)},
  pages={13--24},
  year={2018},
  organization={IEEE}
  }
  
B: Silviu method --> Scheduling real-time communication in IEEE 802.1 Qbv time sensitive networks

  --> @inproceedings{craciunas2016scheduling,
  title={Scheduling real-time communication in IEEE 802.1 Qbv time sensitive networks},
  author={Craciunas, Silviu S and Oliver, Ramon Serna and Chmel{\'\i}k, Martin and Steiner, Wilfried},
  booktitle={Proceedings of the 24th International Conference on Real-Time Networks and Systems},
  pages={183--192},
  year={2016}
  }
C: Niklas method --> Window-based schedule synthesis for industrial IEEE 802.1 Qbv TSN networks

  --> @inproceedings{reusch2020window,
  title={Window-based schedule synthesis for industrial IEEE 802.1 Qbv TSN networks},
  author={Reusch, Niklas and Zhao, Luxi and Craciunas, Silviu S and Pop, Paul},
  booktitle={2020 16th IEEE International Conference on Factory Communication Systems (WFCS)},
  pages={1--4},
  year={2020},
  organization={IEEE}
  }
  
D: Reza method --> Communication scheduling for control performance in TSN-based fog computing platforms

  --> @article{barzegaran2021communication,
  title={Communication scheduling for control performance in TSN-based fog computing platforms},
  author={Barzegaran, Mohammadreza and Pop, Paul},
  journal={IEEE Access},
  volume={9},
  pages={50782--50797},
  year={2021},
  publisher={IEEE}
  }
