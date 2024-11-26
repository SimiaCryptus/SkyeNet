import React from 'react';
import styled from 'styled-components';
import {useDispatch} from 'react-redux';
import {useTheme} from '../../hooks/useTheme';
import {showModal} from '../../store/slices/uiSlice';
import {ThemeName} from '../../themes/themes';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faCog, faHome, faSignInAlt, faSignOutAlt} from '@fortawesome/free-solid-svg-icons';
import {ThemeMenu} from "./ThemeMenu";

const MenuContainer = styled.div`
    display: flex;
    justify-content: space-between;
    padding: ${({theme}) => theme.sizing.spacing.sm};
    background-color: ${({theme}) => theme.colors.surface};
    border-bottom: 1px solid ${({theme}) => theme.colors.border};
`;

const ToolbarLeft = styled.div`
    display: flex;
    gap: ${({theme}) => theme.sizing.spacing.md};
`;

const Dropdown = styled.div`
    position: relative;
    display: inline-block;

    &:hover .dropdown-content {
        display: block;
    }
`;

const DropButton = styled.a`
    color: ${({theme}) => theme.colors.text.primary};
    padding: ${({theme}) => theme.sizing.spacing.sm};
    text-decoration: none;
    cursor: pointer;

    &:hover {
        background-color: ${({theme}) => theme.colors.primary};
        color: white;
    }
`;

const DropdownContent = styled.div`
    display: none;
    position: absolute;
    background-color: ${({theme}) => theme.colors.surface};
    min-width: 160px;
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
    z-index: 1;
`;

const DropdownItem = styled.a`
    color: ${({theme}) => theme.colors.text.primary};
    padding: ${({theme}) => theme.sizing.spacing.sm};
    text-decoration: none;
    display: block;
    cursor: pointer;

    &:hover {
        background-color: ${({theme}) => theme.colors.primary};
        color: white;
    }
`;

export const Menu: React.FC = () => {
    const dispatch = useDispatch();
    const [, setTheme] = useTheme();

    const handleThemeChange = (theme: ThemeName) => {
        setTheme(theme);
    };

    const handleModalOpen = (modalType: string) => {
        dispatch(showModal(modalType));
    };

    return (
        <MenuContainer>
            <ToolbarLeft>
                <DropButton href="/">
                    <FontAwesomeIcon icon={faHome}/> Home
                </DropButton>

                <Dropdown>
                    <DropButton>App</DropButton>
                    <DropdownContent>
                        <DropdownItem onClick={() => handleModalOpen('sessions')}>Session List</DropdownItem>
                        <DropdownItem href="">New</DropdownItem>
                    </DropdownContent>
                </Dropdown>

                <Dropdown>
                    <DropButton>
                        <FontAwesomeIcon icon={faCog}/> Session
                    </DropButton>
                    <DropdownContent>
                        <DropdownItem onClick={() => handleModalOpen('settings')}>Settings</DropdownItem>
                        <DropdownItem onClick={() => handleModalOpen('files')}>Files</DropdownItem>
                        <DropdownItem onClick={() => handleModalOpen('usage')}>Usage</DropdownItem>
                        <DropdownItem onClick={() => handleModalOpen('threads')}>Threads</DropdownItem>
                        <DropdownItem onClick={() => handleModalOpen('share')}>Share</DropdownItem>
                        <DropdownItem onClick={() => handleModalOpen('cancel')}>Cancel</DropdownItem>
                        <DropdownItem onClick={() => handleModalOpen('delete')}>Delete</DropdownItem>
                        <DropdownItem onClick={() => handleModalOpen('verbose')}>Show Verbose</DropdownItem>
                    </DropdownContent>
                </Dropdown>

                <ThemeMenu/>

                <Dropdown>
                    <DropButton>About</DropButton>
                    <DropdownContent>
                        <DropdownItem onClick={() => handleModalOpen('privacy')}>Privacy Policy</DropdownItem>
                        <DropdownItem onClick={() => handleModalOpen('tos')}>Terms of Service</DropdownItem>
                    </DropdownContent>
                </Dropdown>
            </ToolbarLeft>

            <Dropdown>
                <DropButton>
                    <FontAwesomeIcon icon={faSignInAlt}/> Login
                </DropButton>
                <DropdownContent>
                    <DropdownItem onClick={() => handleModalOpen('user-settings')}>Settings</DropdownItem>
                    <DropdownItem onClick={() => handleModalOpen('user-usage')}>Usage</DropdownItem>
                    <DropdownItem>
                        <FontAwesomeIcon icon={faSignOutAlt}/> Logout
                    </DropdownItem>
                </DropdownContent>
            </Dropdown>
        </MenuContainer>
    );
};